package com.atguigu.tingshu.payment.service.impl;

import cn.hutool.core.lang.Assert;
import com.atguigu.tingshu.account.AccountFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.atguigu.tingshu.order.client.OrderFeignClient;
import com.atguigu.tingshu.payment.mapper.PaymentInfoMapper;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wechat.pay.java.service.payments.model.Transaction;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@SuppressWarnings({"all"})
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {


    @Autowired
    private PaymentInfoMapper paymentInfoMapper;
    @Autowired
    private OrderFeignClient orderFeignClient;
    @Autowired
    private AccountFeignClient accountFeignClient;

    /**
     * 保存本地交易记录
     *
     * @param paymentType
     * @param orderNo
     * @return
     */
    @Override
    public PaymentInfo savePaymentInfo(String paymentType, String orderNo, Long userId) {
        //1.判读数据库中是否存在本地交易记录
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, orderNo));
        if (paymentInfo != null) {
            return paymentInfo;
        }
        paymentInfo = new PaymentInfo();
        paymentInfo.setUserId(userId);
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setPayWay(SystemConstant.ORDER_PAY_WAY_WEIXIN);
        paymentInfo.setPaymentStatus(SystemConstant.PAYMENT_STATUS_UNPAID);
        //2.判断支付类型
        if (SystemConstant.PAYMENT_TYPE_ORDER.equals(paymentType)) {
            //如果是订单
            //2.1调用订单微服务获取订单信息
            OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderNo).getData();
            Assert.notNull("订单信息为空{}", orderNo);
            //2.2封装本地交易记录
            if (!orderInfo.getOrderStatus().equals(SystemConstant.ORDER_STATUS_UNPAID)) {
                throw new GuiguException(500, "订单状态有误");
            }
            paymentInfo.setAmount(orderInfo.getOrderAmount());
            paymentInfo.setContent(orderInfo.getOrderTitle());

        }
        if (SystemConstant.PAYMENT_TYPE_RECHARGE.equals(paymentType)) {
            //3如果是充值
            //3.1远程调用账户微服务获取充值信息
            RechargeInfo rechargeInfo = accountFeignClient.getRechargeInfo(orderNo).getData();
            Assert.notNull("充值信息不存在{}", orderNo);
            if (!rechargeInfo.getRechargeStatus().equals(SystemConstant.ORDER_STATUS_UNPAID)) {
                throw new GuiguException(500, "订单信息有误");
            }
            paymentInfo.setAmount(rechargeInfo.getRechargeAmount());
            paymentInfo.setContent("充值:" + rechargeInfo.getRechargeAmount());
        }

        //4保存本地交易信息
        paymentInfoMapper.insert(paymentInfo);

        return paymentInfo;
    }


    /**
     * 支付回调成功以后更新本地交易记录和相关信息
     *
     * @param transaction 交易信息对象
     */

    @GlobalTransactional(rollbackFor = Exception.class)
    @Override
    public void updatePaymentInfo(Transaction transaction) {
        String orderNo = transaction.getOutTradeNo();
        //1.更新本地交易记录
        int flag = paymentInfoMapper.update(
                null,
                new LambdaUpdateWrapper<PaymentInfo>()
                        .eq(PaymentInfo::getOrderNo, orderNo)
                        .eq(PaymentInfo::getPaymentStatus, SystemConstant.PAYMENT_STATUS_UNPAID)
                        .set(PaymentInfo::getOutTradeNo, transaction.getTransactionId())
                        .set(PaymentInfo::getCallbackTime, new Date())
                        .set(PaymentInfo::getCallbackContent, transaction.toString())
                        .set(PaymentInfo::getPaymentStatus, SystemConstant.PAYMENT_STATUS_PAID)
        );
        //更新成功本地交易记录
        if (flag > 0) {
            //根据订单号查询支付类型
            PaymentInfo paymentInfo = paymentInfoMapper
                    .selectOne(
                            new LambdaQueryWrapper<PaymentInfo>()
                                    .eq(PaymentInfo::getOrderNo, orderNo));
            String paymentType = paymentInfo.getPaymentType();
            //判断是订单还是充值
            if (SystemConstant.PAYMENT_TYPE_ORDER.equals(paymentType)) {
                //远程调用订单微服务更新订单状态
                Result result = orderFeignClient.orderPaySuccess(orderNo);
                if (result.getCode().intValue() != 200) {
                    throw new GuiguException(result.getCode(), result.getMessage());
                }
            }
            if (SystemConstant.PAYMENT_TYPE_RECHARGE.equals(paymentType)) {
                //远程调用账户微服务更新充值订单状态并增加商户的余额
                Result result = accountFeignClient.rechargePaySuccess(orderNo);
                if (result.getCode() != 200) {
                    throw new GuiguException(result.getCode(), result.getMessage());
                }

            }
        }
    }
}
