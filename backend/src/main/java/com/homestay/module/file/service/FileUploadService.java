package com.homestay.module.file.service;

import com.homestay.module.file.vo.UploadedFileVO;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/** 文件模块上传业务。 */
public interface FileUploadService {
    UploadedFileVO upload(MultipartFile file);
    List<UploadedFileVO> uploadBatch(MultipartFile[] files);
    UploadedFileVO addUrl(String url);
}
