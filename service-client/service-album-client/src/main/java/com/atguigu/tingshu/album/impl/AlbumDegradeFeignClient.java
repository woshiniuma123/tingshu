package com.atguigu.tingshu.album.impl;


import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AlbumDegradeFeignClient implements AlbumFeignClient {


    @Override
    public Result<AlbumInfo> getAlbumInfo(Long id) {
        log.error("远程调用获取专辑信息失败");
        return null;
    }

    @Override
    public Result<BaseCategoryView> getCategoryViewByCategory3Id(Long category3Id) {
        log.error("远程调用专辑服务获取分类信息失败");
        return null;
    }

    @Override
    public Result<JSONObject> getBaseCategoryListByCategory1Id(Long category1Id) {
        log.error("远程通过分类1id获取分类列表失败");
        return null;
    }

    @Override
    public Result<List<BaseCategory3>> findTopBaseCategory3(Long category1Id) {
        log.error("根据分类1id获取7个指定的专辑信息");
        return null;
    }

    @Override
    public Result<AlbumStatVo> getAlbumStatVo(Long albumId) {
        log.error("根据专辑id查询专辑统计信息失败");
        return null;

    }

    @Override
    public Result<List<BaseCategory1>> findAllCategory1() {
        log.error("获取所有一级分类失败");
        return null;
    }


}
