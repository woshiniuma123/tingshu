package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface UserPaidTrackService extends IService<UserPaidTrack> {


    Map<Long, Integer> userIsPaidTrack(Long userId, Long albumId, List<Long> needCheckPayStatusTrackIdList);

    List<Long> findUserPaidTrackList(Long albumId, Long userId);
}
