package com.atguigu.tingshu.user.client.impl;


import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class UserDegradeFeignClient implements UserFeignClient {

    @Override
    public Result<UserInfoVo> getUserInfo(Long userId) {
        log.error("调用用户远程微服务获取用户信息失败");
        return null;
    }

    @Override
    public Result<Map<Long, Integer>> userIsPaidTrack(Long userId, Long albumId, List<Long> needCheckPayStatusTrackIdList) {
        log.error("调用用户远程微服务获取当前用户购买的专辑失败");
        return null;
    }

    @Override
    public Result<VipServiceConfig> getVipServiceConfig(Long id) {
        log.error("调用用户远程微服务根据套餐id获取套餐详情失败");
        return null;
    }

    @Override
    public Result<Boolean> isPaidAlbum(Long albumId) {
        log.error("调用用户远程微服务获取用户是否购买过该专辑失败");
        return null;
    }

    @Override
    public Result<List<Long>> findUserPaidTrackList(Long albumId) {
        log.error("调用用户远程微服务获取用户已经购买的声音列表失败");
        return null;
    }

    @Override
    public Result savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
        log.error("调用用户远程微服务给用户发放权益失败");
        return null;
    }
}
