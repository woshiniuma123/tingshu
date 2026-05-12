package com.atguigu.tingshu.order.client.impl;


import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.client.OrderFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OrderDegradeFeignClient implements OrderFeignClient {

    private static final Logger log = LoggerFactory.getLogger(OrderDegradeFeignClient.class);

    @Override
    public Result<OrderInfo> getOrderInfo(String orderNo) {
        log.error("远程调用订单微服务获取订单信息失败");
        return null;
    }

    @Override
    public Result orderPaySuccess(String orderNo) {
        log.error("远程调用订单微服务修改订单状态失败");
        return null;
    }
}
