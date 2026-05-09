package com.atguigu.tingshu.user.factory;

import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.user.pattern.DeliveryStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DeliveryFactory {

    @Autowired
    private Map<String, DeliveryStrategy> map;

    public DeliveryStrategy getStrategy(String itemType) {
        if (map.containsKey(itemType)) {
            return map.get(itemType);
        } else {
            throw new GuiguException(500, "暂时不支持该支付类型");
        }
    }
}
