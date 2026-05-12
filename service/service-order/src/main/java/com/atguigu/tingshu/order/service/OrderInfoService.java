package com.atguigu.tingshu.order.service;

import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface OrderInfoService extends IService<OrderInfo> {


    OrderInfoVo trade(TradeVo tradeVo, Long userId, Integer trackCount);

    Map<String, String> submitOrder(OrderInfoVo orderInfoVo, Long userId);


    /**
     * 保存订单信息
     *
     * @param orderInfoVo
     * @param userId
     */
    OrderInfo saveOrder(OrderInfoVo orderInfoVo, Long userId);

    OrderInfo getOrderInfo(String orderNo);

    Page<OrderInfo> findUserPage(Page<OrderInfo> pageInfo, Long userId);

    void cancelOrder(Long orderId);

    void orderPaySuccess(String orderNo);

}
