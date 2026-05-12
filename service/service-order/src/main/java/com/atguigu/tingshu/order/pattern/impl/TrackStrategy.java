package com.atguigu.tingshu.order.pattern.impl;

import cn.hutool.core.lang.Assert;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.order.pattern.AbstractTradeStrategy;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component(SystemConstant.ORDER_ITEM_TYPE_TRACK)
public class TrackStrategy extends AbstractTradeStrategy {
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
        //3.如果付款类型是声音
        //3.1远程调用专辑微服务获取用户未购买的声音列表
        List<TrackInfo> trackInfoList = albumFeignClient.findPaidTrackInfoList(itemId, tradeVo.getTrackCount()).getData();
        Assert.notNull(trackInfoList, "用户已经购买了全部声音");
        //3.2远程调用专辑微服务获取声音所在专辑的相关信息
        AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(trackInfoList.get(0).getAlbumId()).getData();
        //获取到每个声音的价格
        BigDecimal price = albumInfo.getPrice();
        originalAmount = price.multiply(BigDecimal.valueOf(tradeVo.getTrackCount()));
        orderAmount = price.multiply(BigDecimal.valueOf(tradeVo.getTrackCount()));

        List<OrderDetailVo> orderDetailVoList1 = trackInfoList.stream().map(
                trackInfo -> {
                    OrderDetailVo orderDetailVo = new OrderDetailVo();
                    orderDetailVo.setItemName(trackInfo.getTrackTitle());
                    orderDetailVo.setItemId(trackInfo.getId());
                    orderDetailVo.setItemPrice(price);
                    orderDetailVo.setItemUrl(trackInfo.getCoverUrl());
                    return orderDetailVo;
                }
        ).collect(Collectors.toList());

        orderInfoVo.setOrderDetailVoList(orderDetailVoList1);
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
