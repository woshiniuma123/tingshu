package com.atguigu.tingshu.album.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


public interface FileUploadService {
    /**
     * 上传文件接口
     *
     * @param file
     * @return
     */
    public String uploadFile(MultipartFile file);
}
