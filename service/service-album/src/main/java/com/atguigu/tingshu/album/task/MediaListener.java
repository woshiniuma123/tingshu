package com.atguigu.tingshu.album.task;

import cn.hutool.core.collection.CollectionUtil;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.atguigu.tingshu.common.constant.SystemConstant.*;

/**
 * 监听腾讯云点播的声音，异步审核
 */
@Component
@Slf4j
public class MediaListener {


    @Autowired
    private TrackInfoMapper trackInfoMapper;
    @Autowired
    private VodService vodService;

    //    @Scheduled(cron = "0/5 * * * * ?")
    @XxlJob("checkMediaStatus")
    public void checkMediaStatus() {
        log.info("开始进行声音的审核");
        //查询声音表中的正在审核的声音
        List<TrackInfo> trackInfos = trackInfoMapper.selectList(new LambdaQueryWrapper<TrackInfo>()
                .eq(TrackInfo::getStatus, SystemConstant.TRACK_STATUS_REVIEWING)
                .select(TrackInfo::getReviewTaskId, TrackInfo::getId)
                .last("limit 100"));
        try {
            //如果正在审核的声音不是空，则调用腾讯云提供的api去进行任务审核状态查询
            if (CollectionUtil.isNotEmpty(trackInfos)) {
                for (TrackInfo trackInfo : trackInfos) {
                    String taskId = trackInfo.getReviewTaskId();
                    //审核状态查询
                    String suggestion = vodService.reviewStatus(taskId);
                    if ("block".equals(suggestion)) {
                        trackInfo.setStatus(TRACK_STATUS_NO_PASS);
                    } else if ("review".equals(suggestion)) {
                        trackInfo.setStatus(TRACK_STATUS_ARTIFICIAL);
                    } else if ("pass".equals(suggestion)) {
                        trackInfo.setStatus(TRACK_STATUS_PASS);
                    }
                    trackInfoMapper.updateById(trackInfo);
                }
            }
            log.info("审核完毕");
        } catch (Exception e) {
            throw new GuiguException(500, "任务状态查询失败");
        }
    }
}
