package com.atguigu.tingshu.album.impl;


import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
        log.info("远程调用专辑服务获取分类信息失败");
        return null;
    }
}
