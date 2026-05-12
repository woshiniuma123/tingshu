package com.atguigu.tingshu.order.pattern.impl;

import cn.hutool.core.lang.Assert;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.order.pattern.AbstractTradeStrategy;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component(SystemConstant.ORDER_ITEM_TYPE_ALBUM)
public class AlbumStrategy extends AbstractTradeStrategy {

    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private AlbumFeignClient albumFeignClient;


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
        //2.如果专辑购买
        //判断当前用户是否购买过该专辑
        Boolean flag = userFeignClient.isPaidAlbum(itemId).getData();
        if (flag) {
            //该用户购买过该专辑
            throw new GuiguException(500, "您已经购买过该专辑专辑了" + itemId);
        }
        //2.1远程调用专辑微服务根据专辑id获取信息
        AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(itemId).getData();
        Assert.notNull(albumInfo, "该专辑{}的信息为空", itemId);
        //2.2设置专辑原价
        originalAmount = albumInfo.getPrice();
        derateAmount = originalAmount;
        orderAmount = originalAmount;
        //2.3远程调用用户微服务获取当前用户的信息
        UserInfoVo userInfo = userFeignClient.getUserInfo(userId).getData();
        //2.4判断用户是普通用户还是vip用户
        boolean isVIP = false;
        if (userInfo.getIsVip() == 1 && userInfo.getVipExpireTime().after(new Date())) {
            isVIP = true;
        }
        //2.5用户不是vip
        if (!isVIP && albumInfo.getDiscount().intValue() != -1) {
            orderAmount = originalAmount.multiply(albumInfo.getDiscount())
                    .divide(new BigDecimal("10.0"), 2, RoundingMode.HALF_UP);
        }
        //2.6用户是vip
        if (isVIP && albumInfo.getVipDiscount().intValue() != -1) {
            orderAmount = originalAmount.multiply(albumInfo.getVipDiscount())
                    .divide(new BigDecimal("10.0"), 2, RoundingMode.HALF_UP);
        }
        //折扣金额
        derateAmount = originalAmount.subtract(orderAmount);
        OrderDetailVo orderDetailVo = new OrderDetailVo();
        orderDetailVo.setItemUrl(albumInfo.getCoverUrl());
        orderDetailVo.setItemPrice(originalAmount);
        orderDetailVo.setItemId(itemId);
        orderDetailVo.setItemName("专辑名称:" + albumInfo.getAlbumTitle());
        orderDetailVoList.add(orderDetailVo);
        orderInfoVo.setOrderDetailVoList(orderDetailVoList);

        if (orderAmount.compareTo(orderAmount) == 1) {
            OrderDerateVo orderDerateVo = new OrderDerateVo();
            orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_ALBUM_DISCOUNT);
            orderDerateVo.setDerateAmount(derateAmount);
            orderDerateVo.setRemarks("限时减免");
            orderDerateVoList.add(orderDerateVo);
            orderInfoVo.setOrderDerateVoList(orderDerateVoList);
        }
        orderInfoVo.setItemType(itemType);
        orderInfoVo.setOriginalAmount(originalAmount);
        orderInfoVo.setDerateAmount(derateAmount);
        orderInfoVo.setOrderAmount(orderAmount);
        orderInfoVo.setTimestamp(System.currentTimeMillis());
        //生成交易号
        generateTradeNo(orderInfoVo, userId);
        //生成设置签名防止用户篡改数据
        generateSign(orderInfoVo);

        return orderInfoVo;
    }
}
