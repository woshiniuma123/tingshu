package com.atguigu.tingshu.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.atguigu.tingshu.payment.config.WxPayV3Config;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.atguigu.tingshu.payment.service.WxPayService;
import com.atguigu.tingshu.payment.util.PayUtil;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {

    @Autowired
    private PaymentInfoService paymentInfoService;
    @Autowired
    private WxPayV3Config wxPayV3Config;

    @Autowired
    private RSAAutoCertificateConfig autoCertificateConfig;

    /**
     * 通过微信进行商品支付
     *
     * @param userId
     * @param paymentType
     * @param orderNo
     * @return
     */
    @Override
    public Map<String, String> createJsapi(Long userId, String paymentType, String orderNo) {
        try {
            //1.保存本地交易记录
            PaymentInfo paymentInfo = paymentInfoService.savePaymentInfo(paymentType, orderNo, userId);
            //2.判断本地交易记录支付状态
            if (!SystemConstant.PAYMENT_STATUS_UNPAID.equals(paymentInfo.getPaymentStatus())) {
                throw new GuiguException(500, "本地交易记录状态有误");
            }
            //3.对接微信拉起微信支付所需参数
            JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(autoCertificateConfig).build();

            PrepayRequest request = new PrepayRequest();
            Amount amount = new Amount();
            amount.setTotal(1);
            request.setAmount(amount);
            request.setAppid(wxPayV3Config.getAppid());
            request.setMchid(wxPayV3Config.getMerchantId());
            request.setDescription(paymentInfo.getContent());
            request.setNotifyUrl(wxPayV3Config.getNotifyUrl());
            request.setOutTradeNo(orderNo);
            Payer payer = new Payer();
            payer.setOpenid("odo3j4qp-wC3HVq9Z_D9C0cOr0Zs");
            request.setPayer(payer);
            // response包含了调起支付所需的所有参数，可直接用于前端调起支付
            PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request);
            if (response != null) {
                String timeStamp = response.getTimeStamp();
                String packageVal = response.getPackageVal();
                String paySign = response.getPaySign();
                String signType = response.getSignType();
                String nonceStr = response.getNonceStr();
                Map<String, String> map = new HashMap<>();
                map.put("timeStamp", timeStamp);
                map.put("package", packageVal);
                map.put("paySign", paySign);
                map.put("signType", signType);
                map.put("nonceStr", nonceStr);
                return map;
            }

            return null;
        } catch (Exception e) {
            log.error("微信下单失败");
            throw new GuiguException(500, e.getMessage());
        }
    }

    /**
     * 查询支付状态
     *
     * @param orderNo
     * @return
     */
    @Override
    public Boolean queryPayStatus(String orderNo) {
      /*  //1.构建service
        JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(autoCertificateConfig).build();
        //2.构建请求对象
        QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();

        request.setMchid(wxPayV3Config.getMerchantId());
        request.setOutTradeNo(orderNo);
        //3.调用service取查询订单
        Transaction transaction = service.queryOrderByOutTradeNo(request);
        if (transaction != null) {
            Transaction.TradeStateEnum tradeState = transaction.getTradeState();
            if (Transaction.TradeStateEnum.SUCCESS.equals(tradeState)) {
                Integer payerTotal = transaction.getAmount().getPayerTotal();
                //正常应该查询本地的订单金额
                if (payerTotal.intValue() == 1) {
                    return true;
                }
            }
        }
        return false;*/

        //目前无法微信支付
        Transaction transaction = new Transaction();
        transaction.setTransactionId("wx" + IdUtil.getSnowflakeNextId());
        transaction.setOutTradeNo(orderNo);
        paymentInfoService.updatePaymentInfo(transaction);
        return true;


    }

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 微信支付异步回调接口
     *
     * @param request
     * @return
     */
    @Override
    public Map<String, String> wxNotify(HttpServletRequest request) {
        String wechatPaySerial = request.getHeader("Wechatpay-Serial");
        String wechatpayNonce = request.getHeader("Wechatpay-Nonce");
        String wechatSignature = request.getHeader("Wechatpay-Signature");
        String wechatTimestamp = request.getHeader("Wechatpay-Timestamp");
        String requestBody = PayUtil.readData(request);


        // 构造 RequestParam
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(wechatPaySerial)
                .nonce(wechatpayNonce)
                .signature(wechatSignature)
                .timestamp(wechatTimestamp)
                .body(requestBody)
                .build();
        // 初始化 NotificationParser
        NotificationParser parser = new NotificationParser(autoCertificateConfig);
        //解析交易对象
        Transaction transaction = parser.parse(requestParam, Transaction.class);
        if (Transaction.TradeStateEnum.SUCCESS.equals(transaction.getTradeState()) && transaction.getAmount().getPayerTotal().intValue() == 1) {
            //进行幂等性处理防止重复更新本地交易记录等业务
            String transactionId = transaction.getTransactionId();
            String redisKey = "payment:notify:" + transactionId;
            Boolean flag = redisTemplate.opsForValue().setIfAbsent(redisKey, transaction, 25, TimeUnit.HOURS);
            if (flag) {
                //更新本地交易记录 等业务操作
                paymentInfoService.updatePaymentInfo(transaction);

                return Map.of("code", "SUCCESS", "message", "成功");
            }
        }

        return Map.of();
    }
}
