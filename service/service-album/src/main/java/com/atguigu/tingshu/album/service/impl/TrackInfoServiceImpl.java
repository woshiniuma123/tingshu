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
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
            //进行文本审核
            String text = trackInfo.getTrackTitle() + trackInfo.getTrackIntro();
            String suggestion = vodService.AuditText(text);
            if ("block".equals(suggestion)) {
                trackInfo.setStatus(ALBUM_STATUS_NO_PASS);
            } else if ("review".equals(suggestion)) {
                trackInfo.setStatus(ALBUM_STATUS_ARTIFICIAL);
            } else if ("pass".equals(suggestion)) {
                trackInfo.setStatus(ALBUM_STATUS_PASS);
                //TODO 对声音进行审核
                //发起异步审核任务
                String taskId = vodService.auditMedia(trackInfo.getMediaFileId());
                trackInfo.setReviewTaskId(taskId);
                trackInfo.setStatus(TRACK_STATUS_REVIEWING);
            }
            trackInfoMapper.updateById(trackInfo);
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

    /**
     * 根据声音的id查询声音信息
     *
     * @param id
     * @return
     */
    @Override
    public TrackInfo getTrackInfoById(Long id) {
        TrackInfo trackInfo = trackInfoMapper.selectById(id);
        return trackInfo;
    }

    /**
     * 修改声音的信息
     *
     * @param id
     * @param trackInfoVo
     */
    @Override
    public void updateTrackInfo(Long id, TrackInfo trackInfo) {
        boolean isNeedUpdate = false;
        //1.根据id获取到库中的声音信息
        TrackInfo oldTrackInfo = trackInfoMapper.selectById(id);
        //如果满足则说明声音更新了，获取更新后的声音的文件大小，类型，时长
        if (!trackInfo.getMediaFileId().equals(oldTrackInfo.getMediaFileId())) {
            TrackMediaInfoVo mediaInfo = vodService.getMediaInfo(trackInfo.getMediaFileId());
            if (mediaInfo != null) {
                trackInfo.setMediaDuration(BigDecimal.valueOf(mediaInfo.getDuration()));
                trackInfo.setMediaType(mediaInfo.getType());
                trackInfo.setMediaSize(mediaInfo.getSize());
                trackInfo.setMediaUrl(mediaInfo.getMediaUrl());
                trackInfo.setMediaFileId(trackInfo.getMediaFileId());
                //删除腾讯云点播中的旧文件
                vodService.deleteOldMedia(oldTrackInfo.getMediaFileId());
                isNeedUpdate = true;
            }
        }

        //对文本内容和声音内容进行再次审核
        //进行文本审核
        String text = trackInfo.getTrackTitle() + trackInfo.getTrackIntro();
        String suggestion = vodService.AuditText(text);
        if ("block".equals(suggestion)) {
            trackInfo.setStatus(TRACK_STATUS_NO_PASS);
        } else if ("review".equals(suggestion)) {
            trackInfo.setStatus(TRACK_STATUS_ARTIFICIAL);
        } else if ("pass".equals(suggestion)) {
            trackInfo.setStatus(TRACK_STATUS_PASS);
            //对声音进行审核
            if (isNeedUpdate) {
                //发起异步审核任务
                String taskId = vodService.auditMedia(trackInfo.getMediaFileId());
                trackInfo.setReviewTaskId(taskId);
                trackInfo.setStatus(TRACK_STATUS_REVIEWING);
            }
        }

        trackInfoMapper.updateById(trackInfo);
    }

    /**
     * 根据id删除声音及其相关信息
     *
     * @param id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTrackInfo(Long id) {
        //1.根据声音id查询声音信息
        TrackInfo trackInfo = trackInfoMapper.selectById(id);
        Long albumId = trackInfo.getAlbumId();
        Integer orderNum = trackInfo.getOrderNum();
        //2.更新专辑表中的声音数量
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() - 1);
        albumInfoMapper.updateById(albumInfo);
        //3.更新声音表中大于当前orderNum的声音的信息
        trackInfoMapper.update(null, new LambdaUpdateWrapper<TrackInfo>()
                .eq(TrackInfo::getAlbumId, albumId)
                .gt(TrackInfo::getOrderNum, orderNum)
                .setSql("order_num = order_num - 1"));
        trackInfoMapper.deleteById(id);
        //4.根据声音id删除声音统计表中的信息
        trackStatMapper.delete(new LambdaQueryWrapper<TrackStat>().eq(TrackStat::getTrackId, id));
        //5.删除腾讯云点播平台的相关文件
        vodService.deleteOldMedia(trackInfo.getMediaFileId());

    }

}
