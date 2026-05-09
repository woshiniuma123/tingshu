package com.atguigu.tingshu.user.pattern.impl;

import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.user.mapper.UserPaidTrackMapper;
import com.atguigu.tingshu.user.pattern.DeliveryStrategy;
import com.atguigu.tingshu.user.service.UserPaidTrackService;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component(SystemConstant.ORDER_ITEM_TYPE_TRACK)
@Slf4j
public class TrackDelivery implements DeliveryStrategy {
    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;
    @Autowired
    private AlbumFeignClient albumFeignClient;
    @Autowired
    private UserPaidTrackService userPaidTrackService;

    @Override
    public void delivery(UserPaidRecordVo userPaidRecordVo) {
        String itemType = userPaidRecordVo.getItemType();
        List<Long> itemIdList = userPaidRecordVo.getItemIdList();
        String orderNo = userPaidRecordVo.getOrderNo();
        Long userId = userPaidRecordVo.getUserId();
        //如果用户购买的是声音
        //1.1根据订单号查询该声音是否被购买过
        Long count = userPaidTrackMapper.selectCount(new LambdaQueryWrapper<UserPaidTrack>().eq(UserPaidTrack::getUserId, userId).eq(UserPaidTrack::getOrderNo, orderNo));
        if (count > 0) {
            throw new GuiguException(500, "当前订单购买的声音权益已经发放");
        }
        //1.2远程调用专辑微服务根据声音id获取声音信息
        TrackInfo trackInfo = albumFeignClient.getTrackInfoById(itemIdList.get(0)).getData();
        Long albumId = trackInfo.getAlbumId();
        List<UserPaidTrack> userPaidTrackList = itemIdList.stream().map(id -> {
            UserPaidTrack userPaidTrack = new UserPaidTrack();
            userPaidTrack.setTrackId(id);
            userPaidTrack.setOrderNo(orderNo);
            userPaidTrack.setUserId(userId);
            userPaidTrack.setAlbumId(albumId);
            return userPaidTrack;
        }).collect(Collectors.toList());
        //1.3批量保存声音到声音购买表
        userPaidTrackService.saveBatch(userPaidTrackList);
    }
}
