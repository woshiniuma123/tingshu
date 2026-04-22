package com.atguigu.tingshu.account.reciever;

import cn.hutool.core.collection.CollUtil;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;


@Component
@Slf4j
public class AccountReceiver {
    @Autowired
    private UserAccountService userAccountService;


    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.EXCHANGE_USER, durable = "true"),
            value = @Queue(value = MqConst.QUEUE_USER_REGISTER, durable = "true"),
            key = MqConst.ROUTING_USER_REGISTER))
    public void initUserAccount(Map<String, Object> msg, Channel channel, Message message) {
        log.info("用户注册成功，开始初始化账户信息...{}", msg);
        if (CollUtil.isNotEmpty(msg)) {
            //调用账户service去填写账户表以及相关的账户日志表
            userAccountService.initUserAccount(msg);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

}
