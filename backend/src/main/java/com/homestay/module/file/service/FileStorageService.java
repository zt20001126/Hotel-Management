package com.homestay.module.file.service;

import java.io.InputStream;

/** 房型图片存储与访问服务。 */
public interface FileStorageService {
    /** 上传图片并返回可长期保存的对象标识。 */
    String upload(InputStream input, String filename, String contentType);
    /** 将对象标识转换为临时访问地址，原有普通链接保持不变。 */
    String accessUrl(String reference);
}
