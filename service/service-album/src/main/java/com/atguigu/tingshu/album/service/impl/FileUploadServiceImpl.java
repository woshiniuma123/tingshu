package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.atguigu.tingshu.album.config.MinioConstantProperties;
import com.atguigu.tingshu.album.service.FileUploadService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.xml.crypto.Data;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private MinioConstantProperties minioConstantProperties;

    /**
     * 向minio中上传图片
     *
     * @param file
     * @return
     */
    @Override
    public String uploadFile(MultipartFile file) {
        try {
            //1.对图片的尺寸进行校验
            BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
            if (bufferedImage == null) {
                throw new RuntimeException("文件格式错误");
            }
            int height = bufferedImage.getHeight();
            int width = bufferedImage.getWidth();
            if (height > 900 || width > 900) {
                throw new RuntimeException("文件尺寸大小不符合要求");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //TODO 2.对图片的内容是否违规进行校验
        //3.上传至minio
        try {
            // 桶名/日期/uuid.后缀
            String originalFilename = file.getOriginalFilename();
            String folder = DateUtil.today() + "/";
            String fileName = IdUtil.fastSimpleUUID();
            String filePath = folder + fileName + "." + FileNameUtil.extName(originalFilename);
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(minioConstantProperties.getBucketName()).object(filePath).stream(
                                    file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            String url = minioConstantProperties.getEndpointUrl() + "/" + minioConstantProperties.getBucketName() + "/" + filePath;
            return url;
        } catch (Exception e) {
            log.error("上传文件到minio失败");
            throw new RuntimeException(e);
        }

    }
}
