package com.atguigu.tingshu.listener;

import com.atguigu.tingshu.model.user.UserInfo;
import io.xzxj.canal.core.annotation.CanalListener;
import io.xzxj.canal.core.listener.EntryListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
@CanalListener(destination = "tingshuTopic", schemaName = "tingshu_user", tableName = "user_info")
public class UserListener implements EntryListener<UserInfo> {
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 监听user_info表更新
     *
     * @param before
     * @param after
     * @param fields
     */
    @Override
    public void update(UserInfo before, UserInfo after, Set<String> fields) {
        log.info("监听到user_info表更新");
        String redisKey = "user:info:" + after.getId();
        redisTemplate.delete(redisKey);
    }
}
