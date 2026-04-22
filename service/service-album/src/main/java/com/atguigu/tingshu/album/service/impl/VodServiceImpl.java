package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.codec.Base64;
import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.ims.v20201229.ImsClient;
import com.tencentcloudapi.ims.v20201229.models.ImageModerationRequest;
import com.tencentcloudapi.ims.v20201229.models.ImageModerationResponse;
import com.tencentcloudapi.tms.v20201229.TmsClient;
import com.tencentcloudapi.tms.v20201229.models.TextModerationRequest;
import com.tencentcloudapi.tms.v20201229.models.TextModerationResponse;
import com.tencentcloudapi.vod.v20180717.VodClient;
import com.tencentcloudapi.vod.v20180717.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class VodServiceImpl implements VodService {

    @Autowired
    private VodConstantProperties vodConstantProperties;
    @Autowired
    private VodClient vodClient;
    @Autowired
    private TmsClient tmsClient;
    @Autowired
    private ImsClient imsClient;


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

    @Override
    public void deleteOldMedia(String mediaFileId) {
        try {
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DeleteMediaRequest req = new DeleteMediaRequest();
            req.setFileId(mediaFileId);
            // 返回的resp是一个DeleteMediaResponse的实例，与请求对象对应
            vodClient.DeleteMedia(req);
        } catch (TencentCloudSDKException e) {
            log.error("删除云点播文件失败:{e}", e);
            throw new GuiguException(500, "删除云点播文件失败");
        }
    }

    /**
     * 调用腾讯云的TMS对文本进行审核
     *
     * @param text
     * @return
     */
    @Override
    public String AuditText(String text) {
        try {
            // 实例化一个请求对象,每个接口都会对应一个request对象
            TextModerationRequest req = new TextModerationRequest();
            req.setContent(Base64.encode(text));
            // 返回的resp是一个TextModerationResponse的实例，与请求对象对应
            TextModerationResponse resp = tmsClient.TextModeration(req);
            if (resp != null) {

                return resp.getSuggestion().toLowerCase();
            }
        } catch (TencentCloudSDKException e) {
            throw new GuiguException(500, "调用腾讯云的TMS对文本进行审核失败");
        }
        return null;
    }

    /**
     * 调用腾讯云图片内容审核，对图片进行审核
     *
     * @param file
     * @return
     */
    @Override
    public String auditImage(MultipartFile file) {
        try {
            // 实例化一个请求对象,每个接口都会对应一个request对象
            ImageModerationRequest req = new ImageModerationRequest();
            req.setFileContent(Base64.encode(file.getInputStream()));
            // 返回的resp是一个ImageModerationResponse的实例，与请求对象对应
            ImageModerationResponse resp = imsClient.ImageModeration(req);
            if (resp != null) {
                return resp.getSuggestion().toLowerCase();
            }
        } catch (Exception e) {
            throw new GuiguException(500, "调用腾讯云图片内容审核，对图片进行审核失败");
        }
        return null;
    }

    @Override
    public String auditMedia(String mediaFileId) {
        try {
            ReviewAudioVideoRequest req = new ReviewAudioVideoRequest();
            req.setFileId(mediaFileId);
            // 返回的resp是一个ReviewAudioVideoResponse的实例，与请求对象对应
            ReviewAudioVideoResponse resp = vodClient.ReviewAudioVideo(req);
            return resp.getTaskId();
        } catch (TencentCloudSDKException e) {
            throw new GuiguException(500, "调用腾讯云音频内容审核，对音频进行发起审核任务");
        }
    }

    /**
     * 根据任务id查询，声音的审核状态
     *
     * @param taskId
     * @return
     */
    @Override
    public String reviewStatus(String taskId) {
        try {
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DescribeTaskDetailRequest req = new DescribeTaskDetailRequest();
            req.setTaskId(taskId);
            // 返回的resp是一个DescribeTaskDetailResponse的实例，与请求对象对应
            DescribeTaskDetailResponse resp = vodClient.DescribeTaskDetail(req);
            if (resp != null) {
                //如果是音频审核任务
                if ("ReviewAudioVideo".equals(resp.getTaskType())) {
                    if ("FINISH".equals(resp.getStatus())) {
                        ReviewAudioVideoTask reviewAudioVideoTask = resp.getReviewAudioVideoTask();
                        if ("FINISH".equals(reviewAudioVideoTask.getStatus())) {
                            return reviewAudioVideoTask.getOutput().getSuggestion().toLowerCase();
                        }
                    }
                }
            }
            return null;
        } catch (TencentCloudSDKException e) {
            throw new GuiguException(500, "获取审核结果失败");
        }
    }

}
