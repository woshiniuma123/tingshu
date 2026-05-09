package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.WxMaUserService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.atguigu.tingshu.user.factory.DeliveryFactory;
import com.atguigu.tingshu.user.mapper.*;
import com.atguigu.tingshu.user.pattern.DeliveryStrategy;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.user.service.UserPaidTrackService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.atguigu.tingshu.vo.user.UserUpdateVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private WxMaService wxMaService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RabbitService rabbitService;
    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;

    @Override
    public Map<String, String> wxLogin(String code) {
        try {
            WxMaUserService userService = wxMaService.getUserService();
            WxMaJscode2SessionResult sessionInfo = userService.getSessionInfo(code);
            String openid = sessionInfo.getOpenid();
            String sessionKey = sessionInfo.getSessionKey();

            //1.判断当前数据库中是否已经存在该用户
            UserInfo userInfo = userInfoMapper.selectOne(new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getWxOpenId, openid));
            if (userInfo == null) {
                //如果用户为空则创建一个新用户
                userInfo = new UserInfo();
                userInfo.setWxOpenId(openid);
                userInfo.setNickname("用户" + UUID.randomUUID());
                userInfo.setAvatarUrl("http://192.168.200.6:9000/tingshu/2026-04-20/2d023779007f4774bac5195b7c614a41.png");
                userInfoMapper.insert(userInfo);
                //TODO 2.添加用户的账户信息 采用异步方式
                HashMap<String, Object> msg = new HashMap<>();
                msg.put("userId", userInfo.getId());
                msg.put("amount", 100);
                msg.put("title", "新用户赠送");
                msg.put("orderNo", "ZS" + IdUtil.getSnowflake());
                rabbitService.sendMessage(MqConst.EXCHANGE_USER, MqConst.ROUTING_USER_REGISTER, msg);
            }
            //如果已经存在该用户，则将用户的基本信息存入redis
            String token = IdUtil.randomUUID();
            String userKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
            UserInfoVo userInfoVo = BeanUtil.copyProperties(userInfo, UserInfoVo.class);
            redisTemplate.opsForValue().set(userKey, userInfoVo);
            //存入信息成功以后将token返回给前端
            return Map.of("token", token);
        } catch (WxErrorException e) {
            throw new GuiguException(500, "微信登录失败");
        }

    }

    /**
     * 获取用户基本信息
     *
     * @param userId
     * @return
     */
//    @GuiGuCache(prefix = "user:info:")
    @Override
    public UserInfoVo getUserInfo(Long userId) {
        UserInfo userInfo = userInfoMapper.selectById(userId);
        UserInfoVo userInfoVo = BeanUtil.copyProperties(userInfo, UserInfoVo.class);
        return userInfoVo;
    }

    @Override
    public void updateUser(UserUpdateVo userUpdateVo) {
        Long userId = AuthContextHolder.getUserId();
        UserInfo userInfo = BeanUtil.copyProperties(userUpdateVo, UserInfo.class);
        userInfo.setId(userId);
        userInfoMapper.updateById(userInfo);
    }

    /**
     * 判断当前用户是否购买过该专辑
     *
     * @param userId
     * @param albumId
     * @return
     */
    @Override
    public Boolean isPaidAlbum(Long userId, Long albumId) {
        Long count = userPaidAlbumMapper.selectCount(new LambdaQueryWrapper<UserPaidAlbum>().eq(UserPaidAlbum::getUserId, userId).eq(UserPaidAlbum::getAlbumId, albumId));
        if (count <= 0) {
            //说明用户没购买过
            return false;
        }
        return true;
    }

    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;
    @Autowired
    private AlbumFeignClient albumFeignClient;
    @Autowired
    private UserPaidTrackService userPaidTrackService;
    @Autowired
    private VipServiceConfigMapper vipServiceConfigMapper;
    @Autowired
    private UserVipServiceMapper userVipServiceMapper;
    @Autowired
    private DeliveryFactory deliveryFactory;

    /**
     * 新增用户购买记录购买项目类型 1001-专辑 1002-声音 1003-vip会员
     *
     * @param userPaidRecordVo
     */
    @Override
    public void savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
        /*String itemType = userPaidRecordVo.getItemType();
        List<Long> itemIdList = userPaidRecordVo.getItemIdList();
        String orderNo = userPaidRecordVo.getOrderNo();
        Long userId = userPaidRecordVo.getUserId();
        if (SystemConstant.ORDER_ITEM_TYPE_ALBUM.equals(itemType)) {
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
        } else if (SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(itemType)) {
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
        } else if (SystemConstant.ORDER_ITEM_TYPE_VIP.equals(itemType)) {
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
*/
        String itemType = userPaidRecordVo.getItemType();
        DeliveryStrategy strategy = deliveryFactory.getStrategy(itemType);
        strategy.delivery(userPaidRecordVo);
    }
}

