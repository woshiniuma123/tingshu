package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.vo.user.UserListenProcessVo;

import java.math.BigDecimal;
import java.util.Map;

public interface UserListenProcessService {
    /**
     * 从mongo中获取上次声音的进度
     *
     * @param trackId
     * @return
     */
    BigDecimal getTrackBreakSecond(Long trackId);

    /**
     * 获取mongo集合的名称
     *
     * @param userId
     * @return
     */
    String getCollectionName(Long userId);

    void updateListenProcess(UserListenProcessVo userListenProcessVo);

    Map<String, Long> getLatelyTrack(Long userId);
}
