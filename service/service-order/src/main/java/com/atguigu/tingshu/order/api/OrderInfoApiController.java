package com.atguigu.tingshu.order.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "订单管理")
@RestController
@RequestMapping("api/order")
@SuppressWarnings({"all"})
public class OrderInfoApiController {

    @Autowired
    private OrderInfoService orderInfoService;

    @GuiGuLogin
    @Operation(summary = "获取订单详细信息")
    @PostMapping("/orderInfo/trade")
    public Result<OrderInfoVo> trade(@RequestBody TradeVo tradeVo) {
        Long userId = AuthContextHolder.getUserId();
        Integer trackCount = tradeVo.getTrackCount();
        OrderInfoVo orderInfoVo = orderInfoService.trade(tradeVo, userId, trackCount);
        return Result.ok(orderInfoVo);
    }

    @GuiGuLogin
    @Operation(summary = "提交订单")
    @PostMapping("/orderInfo/submitOrder")
    public Result<Map<String, String>> submitOrder(@RequestBody OrderInfoVo orderInfoVo) {
        Long userId = AuthContextHolder.getUserId();

        Map<String, String> map = orderInfoService.submitOrder(orderInfoVo, userId);
        return Result.ok(map);
    }

    @GetMapping("/orderInfo/getOrderInfo/{orderNo}")
    public Result<OrderInfo> getOrderInfo(@PathVariable String orderNo) {
        OrderInfo orderInfo = orderInfoService.getOrderInfo(orderNo);
        return Result.ok(orderInfo);
    }


}

