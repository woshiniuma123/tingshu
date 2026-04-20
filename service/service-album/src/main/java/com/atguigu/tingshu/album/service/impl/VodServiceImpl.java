package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;

import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.vod.v20180717.VodClient;
import com.tencentcloudapi.vod.v20180717.models.DescribeMediaInfosRequest;
import com.tencentcloudapi.vod.v20180717.models.DescribeMediaInfosResponse;
import com.tencentcloudapi.vod.v20180717.models.MediaInfo;
import com.tencentcloudapi.vod.v20180717.models.MediaMetaData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class VodServiceImpl implements VodService {

    @Autowired
    private VodConstantProperties vodConstantProperties;
    @Autowired
    private VodClient vodClient;


    @Override
    public TrackMediaInfoVo getMediaInfo(String fileId) {
        try {
            DescribeMediaInfosRequest req = new DescribeMediaInfosRequest();
            String[] fileIds1 = {fileId};
            req.setFileIds(fileIds1);

            // 返回的resp是一个DescribeMediaInfosResponse的实例，与请求对象对应
            DescribeMediaInfosResponse response = vodClient.DescribeMediaInfos(req);
            if (response != null) {
                MediaInfo mediaInfo = response.getMediaInfoSet()[0];
                MediaMetaData metaData = mediaInfo.getMetaData();
                String media_type = mediaInfo.getBasicInfo().getType();
                return TrackMediaInfoVo.builder()
                        .size(metaData.getSize())
                        .duration(metaData.getDuration())
                        .type(media_type).build();
            }
            return null;
        } catch (TencentCloudSDKException e) {
            throw new GuiguException(500, "获取云点播详细信息失败");
        }
    }
}
