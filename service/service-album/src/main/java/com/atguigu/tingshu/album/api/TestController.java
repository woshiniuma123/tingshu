package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/test")
public class TestController {
    @Autowired
    private AlbumInfoService albumInfoService;
    @Autowired
    private RedissonClient redissonClient;

    /**
     * 将所有过审的专辑id加入到布隆过滤器中
     *
     * @return
     */

    @GetMapping("/album/addAlbum")
    public Result addAlbumToBloom() {
        List<AlbumInfo> list = albumInfoService.list(
                new LambdaQueryWrapper<AlbumInfo>()
                        .eq(AlbumInfo::getStatus, SystemConstant.ALBUM_STATUS_PASS)
        );
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        for (AlbumInfo albumInfo : list) {
            bloomFilter.add(albumInfo.getId());
        }
        return Result.ok();
    }

}
