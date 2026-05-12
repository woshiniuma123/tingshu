package com.atguigu.tingshu.order.receiver;

import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CancelOrderReceiver {
    @Autowired
    private OrderInfoService orderInfoService;

    /**
     * 监听到消息后执行取消订单的方法
     *
     * @param orderId
     * @param channel
     * @param message
     */
    @SneakyThrows
    @RabbitListener(queues = MqConst.QUEUE_CANCEL_ORDER)
    public void cancelOrder(Long orderId, Channel channel, Message message) {
        if (orderId != null) {
            log.info("监听到关闭订单的消息:{}", orderId);
            orderInfoService.cancelOrder(orderId);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
