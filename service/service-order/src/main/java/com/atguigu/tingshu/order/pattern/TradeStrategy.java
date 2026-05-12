package com.atguigu.tingshu.order.pattern;

import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;

public interface TradeStrategy {


    public OrderInfoVo trade(TradeVo tradeVo, Long userId);
}
