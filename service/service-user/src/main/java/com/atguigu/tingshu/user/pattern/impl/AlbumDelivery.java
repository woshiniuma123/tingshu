package com.atguigu.tingshu.user.pattern.impl;

import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.atguigu.tingshu.user.mapper.UserPaidAlbumMapper;
import com.atguigu.tingshu.user.pattern.DeliveryStrategy;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component(SystemConstant.ORDER_ITEM_TYPE_ALBUM)
public class AlbumDelivery implements DeliveryStrategy {
    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;

    @Override
    public void delivery(UserPaidRecordVo userPaidRecordVo) {
        String itemType = userPaidRecordVo.getItemType();
        List<Long> itemIdList = userPaidRecordVo.getItemIdList();
        String orderNo = userPaidRecordVo.getOrderNo();
        Long userId = userPaidRecordVo.getUserId();
        //1.当前购买的是专辑
        //1.1根据订单id查询专辑支付表，查看该专辑是否已经发放权益
        Long count = userPaidAlbumMapper.selectCount(new LambdaQueryWrapper<UserPaidAlbum>().eq(UserPaidAlbum::getUserId, userId).eq(UserPaidAlbum::getOrderNo, orderNo));
        if (count > 0) {
            throw new GuiguException(500, "当前订单购买的专辑权益已经发放");
        }
        //2.1如果没有发放则开始发放权益
        UserPaidAlbum userPaidAlbum = new UserPaidAlbum();
        userPaidAlbum.setAlbumId(itemIdList.get(0));
        userPaidAlbum.setOrderNo(orderNo);
        userPaidAlbum.setUserId(userId);
        userPaidAlbumMapper.insert(userPaidAlbum);
    }
}
