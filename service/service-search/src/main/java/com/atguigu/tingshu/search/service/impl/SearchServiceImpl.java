package com.atguigu.tingshu.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.search.repository.AlbumInfoIndexRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@SuppressWarnings({"all"})
public class SearchServiceImpl implements SearchService {

    @Autowired
    private AlbumFeignClient albumFeignClient;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private AlbumInfoIndexRepository albumInfoIndexRepository;

    /**
     * 根据商品id远程调用商品微服务，用户微服务获取对应信息来上架商品
     *
     * @param albumId
     */
    @Override
    public void upperAlbum(Long albumId) {
        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();
        //1.远程调用专辑微服务获取专辑信息
        AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
        BeanUtil.copyProperties(albumInfo, albumInfoIndex);
        //2.远程调用专辑微服务的获取分类信息
        BaseCategoryView baseCategoryView = albumFeignClient.getCategoryViewByCategory3Id(albumInfo.getCategory3Id()).getData();
        albumInfoIndex.setCategory2Id(baseCategoryView.getCategory2Id());
        albumInfoIndex.setCategory3Id(baseCategoryView.getCategory3Id());
        //3.远程调用用户微服务获取用户基本信息
        UserInfoVo userInfoVo = userFeignClient.getUserInfo(albumInfo.getUserId()).getData();
        albumInfoIndex.setAnnouncerName(userInfoVo.getNickname());
        //TODO 4.远程调用专辑微服务获取专辑统计信息
        //模拟播放量
        Integer playStatNum = RandomUtil.randomInt(200, 300);
        Integer subscribeStatNum = RandomUtil.randomInt(100, 200);
        Integer buyStatNum = RandomUtil.randomInt(50, 100);
        Integer commentStatNum = RandomUtil.randomInt(20, 50);
        albumInfoIndex.setPlayStatNum(playStatNum);
        albumInfoIndex.setSubscribeStatNum(subscribeStatNum);
        albumInfoIndex.setBuyStatNum(buyStatNum);
        albumInfoIndex.setCommentStatNum(commentStatNum);
        Double hotScore = BigDecimal.valueOf(playStatNum).multiply(BigDecimal.valueOf(0.1))
                .add(BigDecimal.valueOf(subscribeStatNum).multiply(BigDecimal.valueOf(0.2)))
                .add(BigDecimal.valueOf(buyStatNum).multiply(BigDecimal.valueOf(0.3)))
                .add(BigDecimal.valueOf(commentStatNum).multiply(BigDecimal.valueOf(0.4))).doubleValue();
        albumInfoIndex.setHotScore(hotScore);

        List<AlbumAttributeValue> albumAttributeValueVoList = albumInfo.getAlbumAttributeValueVoList();
        List<AttributeValueIndex> attributeValueIndexList = albumAttributeValueVoList.stream()
                .map(albumAttributeValue -> BeanUtil.copyProperties(albumAttributeValue, AttributeValueIndex.class))
                .collect(Collectors.toList());
        albumInfoIndex.setAttributeValueIndexList(attributeValueIndexList);

        //5.将封装好的信息加入索引库当中
        albumInfoIndexRepository.save(albumInfoIndex);
    }

    /**
     * 根据id下架相关专辑
     *
     * @param albumId
     */
    @Override
    public void lowerAlbum(Long albumId) {
        albumInfoIndexRepository.deleteById(albumId);
    }
}
