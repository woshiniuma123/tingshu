package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import org.springframework.web.multipart.MultipartFile;

public interface VodService {


    public TrackMediaInfoVo getMediaInfo(String fileId);

    void deleteOldMedia(String mediaFileId);

    String AuditText(String text);

    String auditImage(MultipartFile file);

    String auditMedia(String mediaFileId);

    String reviewStatus(String taskId);
}
