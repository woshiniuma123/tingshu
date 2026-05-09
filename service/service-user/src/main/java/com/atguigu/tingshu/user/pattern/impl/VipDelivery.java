package com.atguigu.tingshu.user.pattern.impl;

import cn.hutool.core.date.DateUtil;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.UserVipService;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.mapper.UserVipServiceMapper;
import com.atguigu.tingshu.user.mapper.VipServiceConfigMapper;
import com.atguigu.tingshu.user.pattern.DeliveryStrategy;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Slf4j
@Component(SystemConstant.ORDER_ITEM_TYPE_VIP)
public class VipDelivery implements DeliveryStrategy {
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private VipServiceConfigMapper vipServiceConfigMapper;
    @Autowired
    private UserVipServiceMapper userVipServiceMapper;

    @Override
    public void delivery(UserPaidRecordVo userPaidRecordVo) {
        String itemType = userPaidRecordVo.getItemType();
        List<Long> itemIdList = userPaidRecordVo.getItemIdList();
        String orderNo = userPaidRecordVo.getOrderNo();
        Long userId = userPaidRecordVo.getUserId();
        //如果购买的是vip套餐
        //1.判断当前用户是否是vip
        boolean isVIP = false;
        //2.获取用户信息
        UserInfo userInfo = userInfoMapper.selectOne(new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getId, userId));
        if (userInfo.getIsVip() == 1 && userInfo.getVipExpireTime().after(new Date())) {
            isVIP = true;
        }
        //3.根据套餐id获取套餐信息
        Long itemId = itemIdList.get(0);
        VipServiceConfig vipServiceConfig = vipServiceConfigMapper.selectById(itemId);
        Integer serviceMonth = vipServiceConfig.getServiceMonth();
        if (!isVIP) {
            //4.如果原来不是vip则添加用户购买vip记录
            UserVipService userVipService = new UserVipService();
            userVipService.setUserId(userId);
            userVipService.setStartTime(new Date());
            userVipService.setExpireTime(DateUtil.offsetMonth(new Date(), serviceMonth));
            userVipService.setOrderNo(orderNo);
            userVipService.setIsAutoRenew(0);
            userVipServiceMapper.insert(userVipService);
            //5.更新用户的信息
            UserInfo user = new UserInfo();
            user.setId(userId);
            user.setIsVip(1);
            user.setVipExpireTime(DateUtil.offsetMonth(new Date(), serviceMonth));
            userInfoMapper.updateById(user);
        } else {
            //6.添加用户的vip购买记录
            UserVipService userVipService = new UserVipService();
            userVipService.setUserId(userId);
            userVipService.setIsAutoRenew(0);
            userVipService.setOrderNo(orderNo);
            userVipService.setStartTime(DateUtil.offsetDay(userInfo.getVipExpireTime(), 1));
            userVipService.setExpireTime(DateUtil.offsetMonth(DateUtil.offsetDay(userInfo.getVipExpireTime(), 1), serviceMonth));
            userVipServiceMapper.insert(userVipService);
            //7.更新用户的vip过期时间
            UserInfo user = new UserInfo();
            user.setId(userId);
            user.setVipExpireTime(DateUtil.offsetMonth(DateUtil.offsetDay(userInfo.getVipExpireTime(), 1), serviceMonth));
            userInfoMapper.updateById(user);
        }
    }
}
