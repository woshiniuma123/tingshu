package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.nacos.common.utils.StringUtils;
import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Map;

import static com.atguigu.tingshu.common.constant.SystemConstant.*;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {

    @Autowired
    private TrackInfoMapper trackInfoMapper;
    @Autowired
    private VodConstantProperties vodConstantProperties;
    @Autowired
    private VodUploadClient vodUploadClient;
    @Autowired
    private AlbumInfoMapper albumInfoMapper;
    @Autowired
    private TrackStatMapper trackStatMapper;
    @Autowired
    private VodService vodService;


    @Override
    public Map<String, String> uploadTrack(MultipartFile file) {
        try {
            //1.上传文件到本地暂存路径
            String tmpFilePath = UploadFileUtil.uploadTempPath(vodConstantProperties.getTempPath(), file);
            //2.上传文件至腾讯云点播
            VodUploadRequest request = new VodUploadRequest();
            request.setMediaFilePath(tmpFilePath);

            VodUploadResponse response = vodUploadClient.upload(vodConstantProperties.getRegion(), request);
            if (response != null) {
                String fileId = response.getFileId();
                String mediaUrl = response.getMediaUrl();
                return Map.of("mediaFileId", fileId, "mediaUrl", mediaUrl);
            }
            return null;
        } catch (Exception e) {
            log.error("上传文件失败：{e}", e.getMessage());
            throw new RuntimeException("上传文件失败", e);
        }

    }

    /**
     * 保存声音信息、给对应的专辑关联的声音数量加1、添加信息统计表信息
     *
     * @param trackInfoVo
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveTrackInfo(TrackInfoVo trackInfoVo) {
        try {
            //根据albumid查询声音对应的专辑
            AlbumInfo albumInfo = albumInfoMapper.selectOne(new LambdaQueryWrapper<AlbumInfo>()
                    .eq(AlbumInfo::getId, trackInfoVo.getAlbumId()));
            //获取用户id
            Long userId = AuthContextHolder.getUserId();
            //1.添加声音信息表数据
            TrackInfo trackInfo = BeanUtil.copyProperties(trackInfoVo, TrackInfo.class);
            trackInfo.setUserId(userId);
            if (!StringUtils.hasText(trackInfo.getCoverUrl())) {
                trackInfo.setCoverUrl(albumInfo.getCoverUrl());
            }
            //给声音表的新增的声音设置序号
            trackInfo.setOrderNum(albumInfo.getIncludeTrackCount() + 1);
            //给声音设置来源
            trackInfo.setSource(TRACK_SOURCE_UPLOAD);
            //从云点播获取上传文件的时长、大小、类型
            TrackMediaInfoVo mediaInfo = vodService.getMediaInfo(trackInfo.getMediaFileId());
            trackInfo.setMediaDuration(BigDecimal.valueOf(mediaInfo.getDuration()));
            trackInfo.setMediaSize(mediaInfo.getSize());
            trackInfo.setMediaType(mediaInfo.getType());
            trackInfo.setStatus(TRACK_STATUS_NO_PASS);
            trackInfoMapper.insert(trackInfo);
            //2.给专辑表的声音数量+1
            albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() + 1);
            albumInfoMapper.updateById(albumInfo);
            //3.给声音信息统计表添加数据
            //获取声音id
            Long trackInfoId = trackInfo.getId();
            this.saveTrackStat(trackInfoId, TRACK_STAT_PLAY, 0);
            this.saveTrackStat(trackInfoId, TRACK_STAT_COLLECT, 0);
            this.saveTrackStat(trackInfoId, TRACK_STAT_PRAISE, 0);
            this.saveTrackStat(trackInfoId, TRACK_STAT_COMMENT, 0);
        } catch (Exception e) {
            throw new GuiguException(500, "保存声音信息及其相关信息失败");
        }
    }

    /**
     * 保存声音统计信息
     *
     * @param trackId
     * @param statType
     * @param statNum
     */
    @Override
    public void saveTrackStat(Long trackId, String statType, Integer statNum) {
        TrackStat trackStat = new TrackStat();
        trackStat.setTrackId(trackId);
        trackStat.setStatType(statType);
        trackStat.setStatNum(statNum);
        trackStatMapper.insert(trackStat);
    }

    /**
     * 分页查询用户的声音列表
     *
     * @param page
     * @param limit
     * @param trackInfoQuery
     * @return
     */
    @Override
    public Page<TrackListVo> findUserTrackPage(Long page, Long limit, TrackInfoQuery trackInfoQuery) {
        //从当前线程中获取用户的id
        Long userId = AuthContextHolder.getUserId();
        trackInfoQuery.setUserId(userId);
        Page<TrackListVo> pageInfo = new Page<>(page, limit);
        pageInfo = trackInfoMapper.findUserTrackPage(pageInfo, trackInfoQuery);
        return pageInfo;
    }
}
