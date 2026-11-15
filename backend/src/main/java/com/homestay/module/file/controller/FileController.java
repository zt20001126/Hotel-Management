package com.homestay.module.file.controller;

import com.homestay.common.result.Result;
import com.homestay.module.file.service.FileUploadService;
import com.homestay.module.file.vo.UploadedFileVO;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/** 文件上传接口。 */
@RestController
@RequestMapping("/file")
public class FileController {
    private final FileUploadService service;
    public FileController(FileUploadService service) { this.service = service; }
    @PostMapping("/upload")
    public Result<UploadedFileVO> upload(@RequestParam("file") MultipartFile file) { return Result.success(service.upload(file)); }
    @PostMapping("/upload/batch")
    public Result<List<UploadedFileVO>> uploadBatch(@RequestParam("files") MultipartFile[] files) { return Result.success(service.uploadBatch(files)); }
    @PostMapping("/add-url")
    public Result<UploadedFileVO> addUrl(@RequestParam("url") String url) { return Result.success(service.addUrl(url)); }
}
