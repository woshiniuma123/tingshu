package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface RechargeInfoService extends IService<RechargeInfo> {

    RechargeInfo getRechargeInfo(String orderNo);

    Map<String, String> submitRecharge(RechargeInfoVo rechargeInfoVo);

    void rechargePaySuccess(String orderNo);

}
