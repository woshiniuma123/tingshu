package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.mapper.BaseCategoryViewMapper;
import com.atguigu.tingshu.album.service.AlbumAttributeValueService;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.vo.album.AlbumAttributeValueVo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.atguigu.tingshu.common.constant.SystemConstant.*;


@Slf4j
@Service
@SuppressWarnings({"all"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

    @Autowired
    private AlbumInfoMapper albumInfoMapper;
    @Autowired
    private AlbumAttributeValueService albumAttributeValueService;
    @Autowired
    private AlbumStatMapper albumStatMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAlbumInfo(AlbumInfoVo albumInfoVo) {
        //1.向albuminfo表中插入信息
        AlbumInfo albumInfo = BeanUtil.copyProperties(albumInfoVo, AlbumInfo.class);
        //1.1从当前线程中获取用户id
        Long userId = AuthContextHolder.getUserId();
        albumInfo.setUserId(userId);
        //1.2判断当前专辑的付费类型
        String payType = albumInfo.getPayType();
        if (ALBUM_PAY_TYPE_REQUIRE.equals(payType) || ALBUM_PAY_TYPE_VIPFREE.equals(payType)) {
            //如果是vip免费和付费的设置可以试听的集数
            albumInfo.setTracksForFree(3);
        }
        //设置审核状态
        albumInfo.setStatus(ALBUM_STATUS_NO_PASS);
        albumInfoMapper.insert(albumInfo);
        Long albumInfoId = albumInfo.getId();
        //2.向albumattributevalue表中插入信息
        List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();
        List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueVoList.stream()
                .map(albumAttributeValueVo -> {
                    AlbumAttributeValue albumAttributeValue = BeanUtil.copyProperties(albumAttributeValueVo, AlbumAttributeValue.class);
                    albumAttributeValue.setAlbumId(albumInfoId);
                    albumAttributeValue.setValueId(albumAttributeValueVo.getValueId());
                    return albumAttributeValue;
                }).collect(Collectors.toList());
        albumAttributeValueService.saveBatch(albumAttributeValueList);
        //3.向albumstate表中插入信息
        this.saveAlbumInfoStat(albumInfoId, ALBUM_STAT_PLAY, 0);
        this.saveAlbumInfoStat(albumInfoId, ALBUM_STAT_SUBSCRIBE, 0);
        this.saveAlbumInfoStat(albumInfoId, ALBUM_STAT_BUY, 0);
        this.saveAlbumInfoStat(albumInfoId, ALBUM_STAT_COMMENT, 0);
    }

    @Override
    public void saveAlbumInfoStat(Long albumId, String statType, Integer statNum) {
        AlbumStat albumStat = new AlbumStat();
        albumStat.setAlbumId(albumId);
        albumStat.setStatType(statType);
        albumStat.setStatNum(statNum);
        albumStatMapper.insert(albumStat);
    }
}
