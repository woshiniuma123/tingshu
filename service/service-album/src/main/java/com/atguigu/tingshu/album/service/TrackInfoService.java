package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface TrackInfoService extends IService<TrackInfo> {

    /**
     * 上传声音到腾讯云点播平台
     *
     * @param file
     * @return
     */
    Map<String, String> uploadTrack(MultipartFile file);

    void saveTrackInfo(TrackInfoVo trackInfoVo);

    void saveTrackStat(Long trackId, String statType, Integer statNum);

    Page<TrackListVo> findUserTrackPage(Long page, Long limit, TrackInfoQuery trackInfoQuery);

    TrackInfo getTrackInfoById(Long id);

    void updateTrackInfo(Long id, TrackInfo trackInfo);

    void removeTrackInfo(Long id);

    Page<AlbumTrackListVo> findAlbumTrackPage(Long albumId, Page<AlbumTrackListVo> pageInfo);

    void updateTrackStat(TrackStatMqVo trackStatMqVo);

    TrackStatVo getTrackStatVo(Long trackId);

}
