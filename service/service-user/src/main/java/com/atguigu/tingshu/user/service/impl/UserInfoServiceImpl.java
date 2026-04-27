package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.WxMaUserService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
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
}
