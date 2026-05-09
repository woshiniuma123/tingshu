package com.atguigu.tingshu.search.service.impl;

import cn.hutool.core.lang.Assert;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.cache.GuiGuCache;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.search.service.ItemService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class ItemServiceImpl implements ItemService {
    @Autowired
    private AlbumFeignClient albumFeignClient;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private Executor threadPoolTaskExecutor;
    @Autowired
    private RedissonClient redissonClient;

    /**
     * 根据专辑id获取专辑的信息，主播的信息，专辑统计信息，专辑分类信息
     *
     * @param albumId
     * @return
     */
    @GuiGuCache(prefix = "search:albumItem:")
    @Override
    public Map<String, Object> getAlbumItem(Long albumId) {

        Map<String, Object> map = new ConcurrentHashMap<>();
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            //1.根据专辑id获取专辑信息
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
            Assert.notNull(albumId, "获取专辑信息失败专辑的id{}", albumInfo.getId());
            map.put("albumInfo", albumInfo);
            return albumInfo;
        }, threadPoolTaskExecutor);


        CompletableFuture<Void> albumStatCompletableFuture = CompletableFuture.runAsync(() -> {
            //2.根据专辑专辑id获取统计信息
            AlbumStatVo albumStatVo = albumFeignClient.getAlbumStatVo(albumId).getData();
            Assert.notNull(albumStatVo, "获取专辑统计信息失败,专辑id为:{}", albumId);
            map.put("albumStatVo", albumStatVo);
        }, threadPoolTaskExecutor);

        CompletableFuture<Void> baseCategoryViewCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            //3.根据分类3id获取分类信息
            BaseCategoryView baseCategoryView =
                    albumFeignClient.getCategoryViewByCategory3Id(albumInfo.getCategory3Id()).getData();
            Assert.notNull(baseCategoryView,
                    "获取分类视图信息失败,专辑id:{},分类id为:{}", albumInfo.getId(), albumInfo.getCategory3Id());
            map.put("baseCategoryView", baseCategoryView);
        }, threadPoolTaskExecutor);

        CompletableFuture<Void> userInfoCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            //4.根据用户id获取用户信息
            UserInfoVo userInfo = userFeignClient.getUserInfo(albumInfo.getUserId()).getData();
            Assert.notNull(userInfo, "获取用户信息失败,用户id为:{}", albumInfo.getUserId());
            map.put("announcer", userInfo);
        }, threadPoolTaskExecutor);

        CompletableFuture.allOf(
                albumInfoCompletableFuture,
                albumStatCompletableFuture,
                baseCategoryViewCompletableFuture,
                userInfoCompletableFuture
        ).orTimeout(1, TimeUnit.SECONDS).join();
        return map;
    }
}
