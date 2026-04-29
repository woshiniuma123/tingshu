package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.mapper.BaseCategory1Mapper;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class AlbumInfoApiController {

    @Autowired
    private AlbumInfoService albumInfoService;
    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @GuiGuLogin
    @Operation(summary = "保存专辑信息")
    @PostMapping("/albumInfo/saveAlbumInfo")
    public Result saveAlbumInfo(@Validated @RequestBody AlbumInfoVo albumInfoVo) {
        albumInfoService.saveAlbumInfo(albumInfoVo);
        return Result.ok();
    }

    @Operation(summary = "分页查询当前用户的专辑信息")
    @GuiGuLogin
    @PostMapping("/albumInfo/findUserAlbumPage/{page}/{limit}")
    public Result<IPage<AlbumListVo>> findUserAlbumPage(@PathVariable Long page,
                                                        @PathVariable Long limit,
                                                        @RequestBody AlbumInfoQuery albumInfoQuery) {
        IPage<AlbumListVo> pageInfo = new Page<>(page, limit);
        Long userId = AuthContextHolder.getUserId();
        albumInfoQuery.setUserId(userId);
        pageInfo = albumInfoService.findUserAlbumPageByUserId(pageInfo, albumInfoQuery);
        return Result.ok(pageInfo);
    }

    @DeleteMapping("albumInfo/removeAlbumInfo/{id}")
    public Result removeAlbumInfo(@PathVariable Long id) {
        albumInfoService.removeAlbumInfo(id);
        return Result.ok();
    }

    @GetMapping("/albumInfo/getAlbumInfo/{id}")
    public Result<AlbumInfo> getAlbumInfo(@PathVariable Long id) {
        AlbumInfo albumInfo = albumInfoService.getAlbumInfoById(id);
        return Result.ok(albumInfo);
    }

    @GuiGuLogin
    @Operation(summary = "更新专辑信息")
    @PutMapping("/albumInfo/updateAlbumInfo/{id}")
    public Result updateAlbumInfo(@PathVariable Long id, @Validated @RequestBody AlbumInfoVo albumInfoVo) {
        albumInfoService.updateAlbumInfo(id, albumInfoVo);
        return Result.ok();
    }

    @GuiGuLogin
    @Operation(summary = "查询当前用户的所有专辑")
    @GetMapping("/albumInfo/findUserAllAlbumList")
    public Result<List<AlbumInfo>> findUserAllAlbumList() {
        Long userId = AuthContextHolder.getUserId();
        List<AlbumInfo> albumInfoList = albumInfoService.findUserAllAlbumList(userId);
        return Result.ok(albumInfoList);
    }

    @Operation(summary = "根据专辑id查询专辑的统计信息")
    @GetMapping("/albumInfo/getAlbumStatVo/{albumId}")
    public Result<AlbumStatVo> getAlbumStatVo(@PathVariable Long albumId) {
        AlbumStatVo albumStatVo = albumInfoService.getAlbumStatVo(albumId);
        return Result.ok(albumStatVo);
    }


}

