package com.atguigu.tingshu.order.pattern.factory;

import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.order.pattern.TradeStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TradeStrategyFactory {

    @Autowired
    private Map<String, TradeStrategy> strategyMap;


    public TradeStrategy getStrategy(String itemType) {
        if (strategyMap.containsKey(itemType)) {
            return strategyMap.get(itemType);
        } else {
            throw new GuiguException(500, "暂时不支持该付款方式");
        }
    }
}
