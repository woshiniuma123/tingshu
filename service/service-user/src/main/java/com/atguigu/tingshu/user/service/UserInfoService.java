package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.atguigu.tingshu.vo.user.UserUpdateVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {

    Map<String, String> wxLogin(String code);

    UserInfoVo getUserInfo(Long userId);

    void updateUser(UserUpdateVo userUpdateVo);

    Boolean isPaidAlbum(Long userId, Long albumId);

    void savePaidRecord(UserPaidRecordVo userPaidRecordVo);
}
