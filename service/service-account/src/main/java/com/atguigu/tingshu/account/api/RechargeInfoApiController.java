package com.atguigu.tingshu.account.api;


import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "充值管理")
@RestController
@RequestMapping("api/account")
@SuppressWarnings({"all"})
public class RechargeInfoApiController {

    @Autowired
    private RechargeInfoService rechargeInfoService;

    /**
     * 根据订单号获得充值信息
     */
    @Operation(summary = "根据订单号获取充值信息")
    @GetMapping("/rechargeInfo/getRechargeInfo/{orderNo}")
    public Result<RechargeInfo> getRechargeInfo(@PathVariable String orderNo) {
        RechargeInfo rechargeInfo = rechargeInfoService.getRechargeInfo(orderNo);
        return Result.ok(rechargeInfo);
    }

    /**
     * 进行余额充值
     *
     * @param rechargeInfoVo
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "提交充值信息")
    @PostMapping("/rechargeInfo/submitRecharge")
    public Result<Map<String, String>> submitRecharge(@RequestBody RechargeInfoVo rechargeInfoVo) {
        Map<String, String> map = rechargeInfoService.submitRecharge(rechargeInfoVo);
        return Result.ok(map);
    }

    @Operation(summary = "修改订单状态并修改账户的余额")
    @GetMapping("/rechargeInfo/rechargePaySuccess/{orderNo}")
    public Result rechargePaySuccess(@PathVariable String orderNo) {
        rechargeInfoService.rechargePaySuccess(orderNo);
        return Result.ok();
    }


}

