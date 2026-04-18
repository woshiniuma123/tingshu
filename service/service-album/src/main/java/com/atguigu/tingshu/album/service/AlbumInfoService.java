package com.atguigu.tingshu.album.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface AlbumInfoService extends IService<AlbumInfo> {


    void saveAlbumInfo(AlbumInfoVo albumInfoVo);

    void saveAlbumInfoStat(Long albumId, String statType, Integer statNum);
}
