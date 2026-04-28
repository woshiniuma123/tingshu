package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "用户声音播放进度管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class UserListenProcessApiController {

    @Autowired
    private UserListenProcessService userListenProcessService;

    @GuiGuLogin(required = false)
    @Operation(summary = "获取声音的上次跳出时间")
    @GetMapping("/userListenProcess/getTrackBreakSecond/{trackId}")
    public Result<BigDecimal> getTrackBreakSecond(@PathVariable Long trackId) {
        BigDecimal bigDecimal = userListenProcessService.getTrackBreakSecond(trackId);
        return Result.ok(bigDecimal);
    }

    @GuiGuLogin(required = false)
    @Operation(summary = "更新播放进度")
    @PostMapping("/userListenProcess/updateListenProcess")
    public Result updateListenProcess(@RequestBody UserListenProcessVo userListenProcessVo) {
        userListenProcessService.updateListenProcess(userListenProcessVo);
        return Result.ok();
    }
}

