package com.atguigu.tingshu.order.pattern;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public abstract class AbstractTradeStrategy implements TradeStrategy {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 生成交易号
     *
     * @param orderInfo
     * @param userId
     */
    public void generateTradeNo(OrderInfoVo orderInfovo, Long userId) {
        String tradeKey = RedisConstant.ORDER_TRADE_NO_PREFIX + userId;
        String tradeNo = IdUtil.randomUUID();
        redisTemplate.opsForValue().set(tradeKey, tradeNo, 5, TimeUnit.MINUTES);
        orderInfovo.setTradeNo(tradeNo);
    }


    public void generateSign(OrderInfoVo orderInfoVo) {
        Map<String, Object> orderInfoMap = BeanUtil.beanToMap(orderInfoVo, false, true);
        String sign = SignHelper.getSign(orderInfoMap);
        orderInfoVo.setSign(sign);
    }

}
