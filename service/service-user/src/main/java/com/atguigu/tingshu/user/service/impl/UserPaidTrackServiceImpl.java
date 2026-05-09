package com.atguigu.tingshu.user.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.user.mapper.UserPaidAlbumMapper;
import com.atguigu.tingshu.user.mapper.UserPaidTrackMapper;
import com.atguigu.tingshu.user.service.UserPaidTrackService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"all"})
public class UserPaidTrackServiceImpl extends ServiceImpl<UserPaidTrackMapper, UserPaidTrack> implements UserPaidTrackService {

    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;
    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;

    /**
     * 获取用户的声音列表付费情况
     *
     * @param userId
     * @param albumId
     * @return
     */
    @Override
    public Map<Long, Integer> userIsPaidTrack(Long userId, Long albumId, List<Long> needCheckPayStatusTrackIdList) {
        Map<Long, Integer> map = new HashMap<>();
        //1.查询当前用户是否购买整张专辑
        Long count = userPaidAlbumMapper.selectCount(
                new LambdaQueryWrapper<UserPaidAlbum>()
                        .eq(UserPaidAlbum::getAlbumId, albumId)
                        .eq(UserPaidAlbum::getUserId, userId)
        );
        if (count > 0) {
            //当前用户购买了整张专辑
            needCheckPayStatusTrackIdList.stream().forEach(trackIds -> map.put(trackIds, 1));
            return map;
        }

        //如果没有购买专辑则查询声音列表
        List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(
                new LambdaQueryWrapper<UserPaidTrack>()
                        .eq(UserPaidTrack::getAlbumId, albumId)
                        .eq(UserPaidTrack::getUserId, userId)
                        .select(UserPaidTrack::getTrackId)
        );
        if (CollectionUtil.isEmpty(userPaidTrackList)) {
            //说明用户没有购买声音
            needCheckPayStatusTrackIdList.stream().forEach(trackid -> map.put(trackid, 0));
            return map;
        }

        //如果购买了
        List<Long> userPaidTrackIdList =
                userPaidTrackList.stream()
                        .map(userPaidTrack -> userPaidTrack.getTrackId())
                        .collect(Collectors.toList());
        for (Long needCheckTrackId : needCheckPayStatusTrackIdList) {
            if (userPaidTrackIdList.contains(needCheckTrackId)) {
                map.put(needCheckTrackId, 1);
            } else {
                map.put(needCheckTrackId, 0);
            }
        }
        return map;
    }

    /**
     * 查询用户已经购买的声音id列表
     *
     * @param albumId
     * @return
     */
    @Override
    public List<Long> findUserPaidTrackList(Long albumId, Long userId) {
        List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(
                new LambdaQueryWrapper<UserPaidTrack>()
                        .eq(UserPaidTrack::getAlbumId, albumId)
                        .eq(UserPaidTrack::getUserId, userId).
                        select(UserPaidTrack::getTrackId)
        );

        if (CollectionUtil.isNotEmpty(userPaidTrackList)) {
            List<Long> userPaidTrackIds = userPaidTrackList.stream()
                    .map(userPaidTrack -> userPaidTrack.getTrackId())
                    .collect(Collectors.toList());
            return userPaidTrackIds;
        }
        return List.of();
    }
}
