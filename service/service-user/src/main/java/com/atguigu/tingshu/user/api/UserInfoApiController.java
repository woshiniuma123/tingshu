package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.user.service.UserPaidTrackService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class UserInfoApiController {

    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private UserPaidTrackService userPaidTrackService;

    @GetMapping("/userInfo/getUserInfoVo/{userId}")
    public Result<UserInfoVo> getUserInfo(@PathVariable("userId") Long userId) {
        UserInfoVo userInfo = userInfoService.getUserInfo(userId);
        return Result.ok(userInfo);
    }

    @Operation(summary = "获取用户声音列表付费情况")
    @PostMapping("/userInfo/userIsPaidTrack/{userId}/{albumId}")
    public Result<Map<Long, Integer>> userIsPaidTrack(
            @PathVariable Long userId,
            @PathVariable Long albumId,
            @RequestBody List<Long> needCheckPayStatusTrackIdList) {
        Map<Long, Integer> map = userPaidTrackService.userIsPaidTrack(userId, albumId, needCheckPayStatusTrackIdList);
        return Result.ok(map);
    }

    @GuiGuLogin
    @Operation(summary = "判断当前用户是否购买过当前专辑")
    @GetMapping("/userInfo/isPaidAlbum/{albumId}")
    public Result<Boolean> isPaidAlbum(@PathVariable Long albumId) {
        Long userId = AuthContextHolder.getUserId();
        Boolean isPaid = userInfoService.isPaidAlbum(userId, albumId);
        return Result.ok(isPaid);
    }

    @GuiGuLogin
    @Operation(summary = "根据专辑id+用户ID获取用户已购买声音id列表")
    @GetMapping("/userInfo/findUserPaidTrackList/{albumId}")
    public Result<List<Long>> findUserPaidTrackList(@PathVariable Long albumId) {
        Long userId = AuthContextHolder.getUserId();
        List<Long> list = userPaidTrackService.findUserPaidTrackList(albumId, userId);
        return Result.ok(list);
    }

    @Operation(summary = "发放用户购买的权益")
    @PostMapping("/userInfo/savePaidRecord")
    public Result savePaidRecord(@RequestBody UserPaidRecordVo userPaidRecordVo) {
        userInfoService.savePaidRecord(userPaidRecordVo);
        return Result.ok();
    }
}

