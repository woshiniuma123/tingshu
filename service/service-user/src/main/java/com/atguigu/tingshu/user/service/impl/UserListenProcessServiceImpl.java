package com.atguigu.tingshu.user.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.user.UserListenProcess;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import com.mongodb.client.MongoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"all"})
public class UserListenProcessServiceImpl implements UserListenProcessService {

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private MongoClient mongo;


    /**
     * 从mongo中获取上次声音的进度
     *
     * @param trackId
     * @return
     */
    @Override
    public BigDecimal getTrackBreakSecond(Long trackId) {
        //1.获取用户的id
        Long userId = AuthContextHolder.getUserId();
        //2.获取集合名称
        if (userId != null) {
            String collectionName = this.getCollectionName(userId);
            Query query = new Query();
            query.addCriteria(Criteria.where("userId").is(userId).and("trackId").is(trackId));
            UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, collectionName);
            if (userListenProcess != null) {
                BigDecimal breakSecond = userListenProcess.getBreakSecond();
                return breakSecond.setScale(0, RoundingMode.HALF_UP);
            }
        }
        return BigDecimal.ZERO;
    }


    @Override
    public String getCollectionName(Long userId) {
        return "UserListenProcess_" + userId;
    }

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RabbitService rabbitService;

    /**
     * 更新播放进度
     *
     * @param userListenProcessVo
     */
    @Override
    public void updateListenProcess(UserListenProcessVo userListenProcessVo) {

        Long userId = AuthContextHolder.getUserId();

        if (userId != null) {
            String collectionName = this.getCollectionName(userId);
            Query query = new Query();
            query.addCriteria(Criteria.where("userId")
                    .is(userId).and("albumId").
                    is(userListenProcessVo.getAlbumId())
                    .and("trackId").is(userListenProcessVo.getTrackId()));

            UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, collectionName);
            BigDecimal newBreakSeconds = userListenProcessVo.getBreakSecond().setScale(0, RoundingMode.HALF_UP);
            if (userListenProcess == null) {
                //保存
                UserListenProcess process = new UserListenProcess();
                process.setUserId(userId);
                process.setAlbumId(userListenProcessVo.getAlbumId());
                process.setTrackId(userListenProcessVo.getTrackId());
                process.setBreakSecond(newBreakSeconds);
                process.setCreateTime(new Date());
                process.setUpdateTime(new Date());
                process.setIsShow(1);
                mongoTemplate.save(process, collectionName);
            } else {
                userListenProcess.setUpdateTime(new Date());
                userListenProcess.setBreakSecond(newBreakSeconds);
                mongoTemplate.save(userListenProcess, collectionName);
            }
            //更新mysql的统计信息，每个用户只允许每天更新一次 关键点找出业务标识作为RedisKey：前缀:用户ID_专辑ID_声音ID
            String redisKey = RedisConstant.USER_TRACK_REPEAT_STAT_PREFIX
                    + userId + "_" + userListenProcessVo.getAlbumId() + "_" + userListenProcessVo.getTrackId();
            //当日的
            long end = DateUtil.endOfDay(new Date()).getTime();
            long now = new DateTime(new Date()).getTime();
            long ttl = end - now;
            Boolean flag = redisTemplate.opsForValue().setIfAbsent(redisKey, userListenProcessVo.getTrackId(), ttl, TimeUnit.MILLISECONDS);
            if (flag) {
                //证明改用户是今天第一次收听这个声音，通过rabbit发送修改统计信息的消息
                TrackStatMqVo trackStatMqVo = new TrackStatMqVo();
                trackStatMqVo.setAlbumId(userListenProcessVo.getAlbumId());
                trackStatMqVo.setCount(1);
                trackStatMqVo.setTrackId(userListenProcessVo.getTrackId());
                trackStatMqVo.setStatType(SystemConstant.TRACK_STAT_PLAY);
                trackStatMqVo.setBusinessNo(IdUtil.fastUUID());
                rabbitService.sendMessage(MqConst.EXCHANGE_TRACK, MqConst.ROUTING_TRACK_STAT_UPDATE, trackStatMqVo);
            }

        }
    }

    /**
     * '获取用户最近一次播放的声音记录
     *
     * @param userId
     * @return
     */
    @Override
    public Map<String, Long> getLatelyTrack(Long userId) {
        HashMap<String, Long> map = new HashMap<>();
        String collectionName = this.getCollectionName(userId);
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId));
        query.with(Sort.by(Sort.Direction.DESC, "createTime"));

        UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, collectionName);

        if (userListenProcess != null) {
            map.put("trackId", userListenProcess.getTrackId());
            map.put("albumId", userListenProcess.getAlbumId());
            return map;
        }
        return map;
    }


}
