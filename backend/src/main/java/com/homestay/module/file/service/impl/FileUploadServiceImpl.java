package com.homestay.module.file.service.impl;

import com.homestay.common.exception.BusinessException;
import com.homestay.module.file.service.FileStorageService;
import com.homestay.module.file.service.FileUploadService;
import com.homestay.module.file.vo.UploadedFileVO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/** 协调房型图片上传和访问地址生成。 */
@Service
public class FileUploadServiceImpl implements FileUploadService {
    private final FileStorageService storage;
    public FileUploadServiceImpl(FileStorageService storage) { this.storage = storage; }

    @Override
    public UploadedFileVO upload(MultipartFile file) {
        if (file.isEmpty()) { throw new BusinessException("请选择要上传的文件"); }
        try (var input = file.getInputStream()) {
            String ref = storage.upload(input, file.getOriginalFilename(), file.getContentType());
            return new UploadedFileVO(storage.accessUrl(ref), file.getOriginalFilename(), ref);
        } catch (IOException e) {
            throw new BusinessException("读取上传文件失败");
        }
    }
    @Override
    public List<UploadedFileVO> uploadBatch(MultipartFile[] files) {
        return Arrays.stream(files).filter(file -> !file.isEmpty()).map(this::upload).toList();
    }
    @Override
    public UploadedFileVO addUrl(String url) {
        if (url == null || !(url.trim().startsWith("https://") || url.trim().startsWith("http://"))) {
            throw new BusinessException("无效的图片URL格式");
        }
        return new UploadedFileVO(url.trim(), "网络图片", url.trim());
    }
}
