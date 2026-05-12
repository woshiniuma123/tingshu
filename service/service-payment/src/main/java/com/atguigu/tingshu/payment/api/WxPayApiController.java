package com.atguigu.tingshu.payment.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.payment.service.WxPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "微信支付接口")
@RestController
@RequestMapping("api/payment")
@Slf4j
public class WxPayApiController {

    @Autowired
    private WxPayService wxPayService;

    @Operation(summary = "选择微信进行商品支付")
    @GuiGuLogin
    @PostMapping("/wxPay/createJsapi/{paymentType}/{orderNo}")
    public Result<Map<String, String>> createJsapi(@PathVariable String paymentType, @PathVariable String orderNo) {
        Long userId = AuthContextHolder.getUserId();
        Map<String, String> reultMap = wxPayService.createJsapi(userId, paymentType, orderNo);

        return Result.ok(reultMap);
    }

    @Operation(summary = "支付状态查询")
    @GetMapping("/wxPay/queryPayStatus/{orderNo}")
    public Result<Boolean> queryPayStatus(@PathVariable String orderNo) {
        Boolean flag = wxPayService.queryPayStatus(orderNo);
        return Result.ok(flag);
    }

    @Operation(summary = "微信支付成功异步回调")
    @PostMapping("/wxpay/notify")
    public Result<Map<String, String>> wxNotify(HttpServletRequest request) {
        Map<String, String> map = wxPayService.wxNotify(request);
        return Result.ok(map);
    }
}
