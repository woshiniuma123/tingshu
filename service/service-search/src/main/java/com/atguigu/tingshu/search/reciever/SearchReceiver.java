package com.atguigu.tingshu.search.reciever;

import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.search.service.SearchService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class SearchReceiver {

    @Autowired
    private SearchService searchService;

    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.EXCHANGE_ALBUM, durable = "true"),
            value = @Queue(value = MqConst.QUEUE_ALBUM_UPPER, durable = "true"),
            key = MqConst.ROUTING_ALBUM_UPPER))
    public void upperAlbum(Long albumInfoId, Channel channel, Message message) {
        log.info("专辑审核通过，开始对专辑进行上架,专辑id：{}", albumInfoId);
        try {
            if (albumInfoId != null) {
                searchService.upperAlbum(albumInfoId);
            }

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            throw new GuiguException(500, "专辑上架失败");
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.EXCHANGE_ALBUM, durable = "true"),
            value = @Queue(value = MqConst.QUEUE_ALBUM_LOWER, durable = "true"),
            key = MqConst.ROUTING_ALBUM_LOWER))
    public void lowerAlbum(Long albumInfoId, Channel channel, Message message) {
        log.error("专辑审核未通过，开始对专辑进行下架,专辑id：{}", albumInfoId);
        try {
            if (albumInfoId != null) {
                searchService.lowerAlbum(albumInfoId);
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            throw new GuiguException(500, "专辑下架失败");
        }
    }
}
