package com.atguigu.tingshu.album.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class AlbumInfoApiController {

    @Autowired
    private AlbumInfoService albumInfoService;
    @Operation(summary = "获取1,2,3级分类")
    @GetMapping("/category/getBaseCategoryList")
    public Result<List<JSONObject>> getBaseCategoryList() {
        List<JSONObject> list = albumInfoService.getBaseCategoryList();
        return Result.ok(list);
    }

}

