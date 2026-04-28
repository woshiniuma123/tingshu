package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "声音管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class TrackInfoApiController {

    @Autowired
    private TrackInfoService trackInfoService;

    /**
     * 上传声音到腾讯云点播平台
     *
     * @param file
     * @return
     */
    @Operation(summary = "上传声音信息到腾讯云点播平台")
    @PostMapping("/trackInfo/uploadTrack")
    public Result<Map<String, String>> uploadTrack(@RequestParam MultipartFile file) {
        Map<String, String> resultMap = trackInfoService.uploadTrack(file);
        return Result.ok(resultMap);
    }

    @GuiGuLogin
    @Operation(summary = "保存声音信息")
    @PostMapping("/trackInfo/saveTrackInfo")
    public Result saveTrackInfo(@RequestBody TrackInfoVo trackInfoVo) {
        trackInfoService.saveTrackInfo(trackInfoVo);
        return Result.ok();
    }

    @GuiGuLogin
    @Operation(summary = "分页查询当前用户上传的声音")
    @PostMapping("/trackInfo/findUserTrackPage/{page}/{limit}")
    public Result<Page<TrackListVo>> findUserTrackPage(@PathVariable Long page,
                                                       @PathVariable Long limit,
                                                       @RequestBody TrackInfoQuery trackInfoQuery) {
        Page<TrackListVo> pageModel = trackInfoService.findUserTrackPage(page, limit, trackInfoQuery);

        return Result.ok(pageModel);
    }

    @Operation(summary = "根据声音id查询声音的信息")
    @GetMapping("/trackInfo/getTrackInfo/{id}")
    public Result<TrackInfo> getTrackInfoById(@PathVariable Long id) {
        TrackInfo trackInfo = trackInfoService.getTrackInfoById(id);
        return Result.ok(trackInfo);
    }

    @Operation(summary = "根据声音id修改声音的信息")
    @PutMapping("/trackInfo/updateTrackInfo/{id}")
    public Result updateTrackInfo(@PathVariable Long id, @RequestBody TrackInfo trackInfo) {
        trackInfoService.updateTrackInfo(id, trackInfo);
        return Result.ok();
    }

    @Operation(summary = "根据id删除声音及其相关信息")
    @DeleteMapping("/trackInfo/removeTrackInfo/{id}")
    public Result removeTrackInfo(@PathVariable Long id) {
        trackInfoService.removeTrackInfo(id);
        return Result.ok();
    }

    @GuiGuLogin(required = false)
    @Operation(summary = "查询声音分页列表")
    @GetMapping("/trackInfo/findAlbumTrackPage/{albumId}/{page}/{limit}")
    public Result<IPage<AlbumTrackListVo>> findAlbumTrackPage(@PathVariable Long albumId, @PathVariable Integer page, @PathVariable Integer limit) {
        Page<AlbumTrackListVo> pageInfo = new Page<>(page, limit);
        pageInfo = trackInfoService.findAlbumTrackPage(albumId, pageInfo);

        return Result.ok(pageInfo);

    }
}

