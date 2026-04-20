package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.mapper.BaseCategoryViewMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.service.AlbumAttributeValueService;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.*;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumAttributeValueVo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;
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
    @Autowired
    private TrackInfoMapper trackInfoMapper;

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

    @Override
    public IPage<AlbumListVo> findUserAlbumPageByUserId(IPage<AlbumListVo> pageInfo, AlbumInfoQuery albumInfoQuery) {
        IPage<AlbumListVo> page = albumInfoMapper.findUserAlbumPageByUserId(pageInfo, albumInfoQuery);
        return page;
    }

    /**
     * 根据专辑id删除专辑信息以及其相关信息
     *
     * @param id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAlbumInfo(Long id) {
        //1.先检查该专辑下是否有关联的声音
        Long count = trackInfoMapper.selectCount(new LambdaQueryWrapper<TrackInfo>()
                .eq(TrackInfo::getAlbumId, id).eq(TrackInfo::getIsDeleted, 0));
        if (count > 0) {
            //有关联的声音
            throw new RuntimeException("该专辑下有关联的声音，请先删除该专辑下的声音");
        }
        try {
            //2.如果没有关联的声音则删除专辑信息
            albumInfoMapper.deleteById(id);
            //3.删除专辑属性值关联表的信息
            albumAttributeValueService.remove(new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, id));
            //4.删除专辑统计信息
            albumStatMapper.delete(new LambdaQueryWrapper<AlbumStat>().eq(AlbumStat::getAlbumId, id));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据id获取专辑信息和属性值
     *
     * @param id
     * @return
     */
    @Override
    public AlbumInfo getAlbumInfoById(Long id) {
        AlbumInfo albumInfo = albumInfoMapper.getAlbumInfoById(id);
        return albumInfo;
    }

    /**
     * 更新专辑信息
     *
     * @param id
     * @param albumInfoVo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAlbumInfo(Long id, AlbumInfoVo albumInfoVo) {

        //1.更新专辑信息表
        //1.1 从当前线程中获取用户id
        Long userId = AuthContextHolder.getUserId();
        //1.2将albuminfoVo中的属性值复制到AlbumInfo中
        AlbumInfo albumInfo = BeanUtil.copyProperties(albumInfoVo, AlbumInfo.class);
        albumInfo.setId(id);
        albumInfo.setUserId(userId);
        //专辑状态改为未审核
        albumInfo.setStatus(ALBUM_STATUS_NO_PASS);
        //1.3根据id更新专辑信息表
        albumInfoMapper.updateById(albumInfo);
        //2.更新专辑属性值关联表
        //2.1获取albuminfoVo中的AlbumAttributeValueVo类型的列表
        List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();
        List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueVoList.stream()
                .map(albumAttributeValueVo -> {
                    AlbumAttributeValue albumAttributeValue = BeanUtil.copyProperties(albumAttributeValueVo, AlbumAttributeValue.class);
                    albumAttributeValue.setAlbumId(id);
                    return albumAttributeValue;
                }).collect(Collectors.toList());
        //2.2删除专辑属性值关联表信息根据专辑id
        albumAttributeValueService.remove(new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, id));
        albumAttributeValueService.saveBatch(albumAttributeValueList);
    }

    /**
     * 查询当前用户所有的专辑列表
     *
     * @param userId
     * @return
     */
    @Override
    public List<AlbumInfo> findUserAllAlbumList(Long userId) {
        List<AlbumInfo> albumInfos = albumInfoMapper
                .selectList(new LambdaQueryWrapper<AlbumInfo>()
                        .select(AlbumInfo::getId, AlbumInfo::getAlbumTitle)
                        .eq(AlbumInfo::getUserId, userId)
                        .eq(AlbumInfo::getIsDeleted, 0)
                        .orderByDesc(AlbumInfo::getId)
                        .last("limit 200"));
        return albumInfos;
    }


}
