package com.atguigu.tingshu.order.pattern.impl;

import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.order.pattern.AbstractTradeStrategy;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component(SystemConstant.ORDER_ITEM_TYPE_VIP)
public class VIPStrategy extends AbstractTradeStrategy {
    @Autowired
    private UserFeignClient userFeignClient;

    @Override
    public OrderInfoVo trade(TradeVo tradeVo, Long userId) {
        //付款类型id
        Long itemId = tradeVo.getItemId();
        //专辑付款类型 付款项目类型: 1001-专辑 1002-声音 1003-vip会员
        String itemType = tradeVo.getItemType();
        //0.0初始化orderinfoVo
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        //0.1初始化订单明细列表
        List<OrderDetailVo> orderDetailVoList = new ArrayList<>();
        //0.2初始化订单减免列表
        List<OrderDerateVo> orderDerateVoList = new ArrayList<>();
        //0.3初始化订单原始金额
        BigDecimal originalAmount = new BigDecimal("0.00");
        BigDecimal derateAmount = new BigDecimal("0.00");
        BigDecimal orderAmount = new BigDecimal("0.00");

        VipServiceConfig vipServiceConfig = userFeignClient.getVipServiceConfig(itemId).getData();
        originalAmount = vipServiceConfig.getPrice();
        orderAmount = vipServiceConfig.getDiscountPrice();
        derateAmount = originalAmount.subtract(orderAmount);
        //构建订单明细
        OrderDetailVo orderDetailVo = new OrderDetailVo();
        orderDetailVo.setItemId(itemId);
        orderDetailVo.setItemName(vipServiceConfig.getName());
        orderDetailVo.setItemPrice(originalAmount);
        orderDetailVo.setItemUrl(vipServiceConfig.getImageUrl());
        orderDetailVoList.add(orderDetailVo);
        orderInfoVo.setOrderDetailVoList(orderDetailVoList);

        if (originalAmount.compareTo(orderAmount) == 1) {
            //构建订单折扣列表
            OrderDerateVo orderDerateVo = new OrderDerateVo();
            orderDerateVo.setDerateAmount(derateAmount);
            orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_VIP_SERVICE_DISCOUNT);
            orderDerateVo.setRemarks("VIP充值折扣");
            orderDerateVoList.add(orderDerateVo);
            orderInfoVo.setOrderDerateVoList(orderDerateVoList);
        }
        orderInfoVo.setItemType(itemType);
        orderInfoVo.setOriginalAmount(originalAmount);
        orderInfoVo.setDerateAmount(derateAmount);
        orderInfoVo.setOrderAmount(orderAmount);
        orderInfoVo.setTimestamp(System.currentTimeMillis());
        generateTradeNo(orderInfoVo, userId);
        generateSign(orderInfoVo);

        return orderInfoVo;
    }
}
