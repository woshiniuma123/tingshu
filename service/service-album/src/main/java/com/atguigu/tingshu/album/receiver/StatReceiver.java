package com.atguigu.tingshu.album.receiver;

import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class StatReceiver {
    private static final Logger log = LoggerFactory.getLogger(StatReceiver.class);
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private TrackInfoService trackInfoService;


    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.EXCHANGE_TRACK, durable = "true"),
            value = @Queue(value = MqConst.QUEUE_TRACK_STAT_UPDATE, durable = "true"),
            key = MqConst.QUEUE_TRACK_STAT_UPDATE))
    public void updateStat(TrackStatMqVo mqVo, Channel channel, Message message) {
        try {
            if (mqVo != null) {
                log.info("监听到消息trackId:{}", mqVo.getTrackId());
                String redisKey = RedisConstant.USER_TRACK_REPEAT_STAT_PREFIX + "db:" + mqVo.getBusinessNo();
                Boolean flag = redisTemplate.opsForValue().setIfAbsent(redisKey, mqVo.getTrackId(), 10, TimeUnit.MINUTES);
                if (flag) {
                    trackInfoService.updateTrackStat(mqVo);
                }
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            try {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            } catch (IOException ex) {
                throw new GuiguException(500, ex.getMessage());
            }
            throw new GuiguException(500, e.getMessage());
        }
    }
}
