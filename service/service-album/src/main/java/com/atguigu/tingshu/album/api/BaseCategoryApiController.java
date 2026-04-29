package com.atguigu.tingshu.album.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "分类管理")
@RestController
@RequestMapping(value = "/api/album")
@SuppressWarnings({"all"})
public class BaseCategoryApiController {

    @Autowired
    private BaseCategoryService baseCategoryService;

    @Operation(summary = "获取1,2,3级分类")
    @GetMapping("/category/getBaseCategoryList")
    public Result<List<JSONObject>> getBaseCategoryList() {
        List<JSONObject> list = baseCategoryService.getBaseCategoryList();
        System.out.println("====");
        System.out.println("====");
        System.out.println("====");
        System.out.println("====");

        return Result.ok(list);
    }

    @Operation(summary = "根据一级分类Id获取分类属性以及属性值（标签名，标签值）列表")
    @GetMapping("/category/findAttribute/{category1Id}")
    public Result<List<JSONObject>> getAttributeByCategory1Id(@PathVariable("category1Id") Long category1Id) {
        List<JSONObject> list = baseCategoryService.getAttributeByCategory1Id(category1Id);
        return Result.ok(list);
    }

    @Operation(summary = "根据三级分类Id 获取到分类信息")
    @GetMapping("/category/getCategoryView/{category3Id}")
    public Result<BaseCategoryView> getCategoryViewByCategory3Id(@PathVariable Long category3Id) {
        BaseCategoryView categoryView = baseCategoryService.getCategoryViewByCategory3Id(category3Id);
        return Result.ok(categoryView);
    }

    @GetMapping("/category/findTopBaseCategory3/{category1Id}")
    public Result<List<BaseCategory3>> findTopBaseCategory3(@PathVariable Long category1Id) {
        List<BaseCategory3> category3 = baseCategoryService.findTopBaseCategory3(category1Id);
        return Result.ok(category3);
    }

    @Operation(summary = "根据一级分类id查询分类的列表")
    @GetMapping("/category/getBaseCategoryList/{category1Id}")
    public Result<JSONObject> getBaseCategoryListByCategory1Id(@PathVariable Long category1Id) {
        JSONObject cateGory1View = baseCategoryService.getBaseCategoryListByCategory1Id(category1Id);
        return Result.ok(cateGory1View);
    }

    @Operation(summary = "查询所有一级分类信息")
    @GetMapping("/category/findAllCategory1")
    public Result<List<BaseCategory1>> findAllCategory1() {
        List<BaseCategory1> baseCategory1List = baseCategoryService.list();
        return Result.ok(baseCategory1List);
    }

}

