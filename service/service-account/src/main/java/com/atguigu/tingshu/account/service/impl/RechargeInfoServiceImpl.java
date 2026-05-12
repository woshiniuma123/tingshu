package com.atguigu.tingshu.account.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.account.mapper.RechargeInfoMapper;
import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@SuppressWarnings({"all"})
public class RechargeInfoServiceImpl extends ServiceImpl<RechargeInfoMapper, RechargeInfo> implements RechargeInfoService {

    @Autowired
    private RechargeInfoMapper rechargeInfoMapper;
    @Autowired
    private UserAccountService userAccountService;

    /**
     * 获取充值
     *
     * @param orderNo
     * @return
     */
    @Override
    public RechargeInfo getRechargeInfo(String orderNo) {
        RechargeInfo rechargeInfo = rechargeInfoMapper.selectOne(
                new LambdaQueryWrapper<RechargeInfo>()
                        .eq(RechargeInfo::getOrderNo, orderNo)
        );
        return rechargeInfo;
    }

    /**
     * 进行余额充值
     *
     * @param rechargeInfoVo
     * @return
     */
    @Override
    public Map<String, String> submitRecharge(RechargeInfoVo rechargeInfoVo) {
        Long userId = AuthContextHolder.getUserId();
        RechargeInfo rechargeInfo = new RechargeInfo();
        rechargeInfo.setUserId(userId);
        String orderNo = "CZ" + DateUtil.today().replace("-", "") + IdUtil.getSnowflakeNextId();
        rechargeInfo.setOrderNo(orderNo);
        rechargeInfo.setPayWay(rechargeInfoVo.getPayWay());
        rechargeInfo.setRechargeAmount(rechargeInfoVo.getAmount());
        rechargeInfo.setRechargeStatus(SystemConstant.ORDER_STATUS_UNPAID);
        rechargeInfoMapper.insert(rechargeInfo);

        //订单超时进行延迟关闭订单
        return Map.of("orderNo", orderNo);
    }

    /**
     * 更新充值状态并更新账户信息
     *
     * @param orderNo
     */
    @Override
    public void rechargePaySuccess(String orderNo) {
        int flag = rechargeInfoMapper.update(null,
                new LambdaUpdateWrapper<RechargeInfo>()
                        .eq(RechargeInfo::getOrderNo, orderNo)
                        .eq(RechargeInfo::getRechargeStatus, SystemConstant.ORDER_STATUS_UNPAID)
                        .set(RechargeInfo::getRechargeStatus, SystemConstant.ORDER_STATUS_PAID)
        );

        if (flag > 0) {
            //1.根据订单号查询充值信息
            RechargeInfo rechargeInfo = rechargeInfoMapper.selectOne(
                    new LambdaQueryWrapper<RechargeInfo>()
                            .eq(RechargeInfo::getOrderNo, orderNo)
            );
            Long userId = rechargeInfo.getUserId();
            BigDecimal rechargeAmount = rechargeInfo.getRechargeAmount();
            //更新用户的账户信息
            userAccountService.updateUserAccount(userId, rechargeAmount);
            //新增账户变动日志
            UserAccountDetail userAccountDetail = new UserAccountDetail();
            userAccountDetail.setAmount(rechargeAmount);
            userAccountDetail.setOrderNo(orderNo);
            userAccountDetail.setTitle("用户充值");
            userAccountDetail.setUserId(userId);
            userAccountDetail.setTradeType(SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT);
            userAccountService.saveUserAccountDetail(userAccountDetail);
        }
    }
}
