package com.homestay.module.file.vo;

/** 文件上传结果，供管理后台预览与保存。 */
public record UploadedFileVO(
        /** 图片临时访问地址。 */ String url,
        /** 原始文件名。 */ String name,
        /** 可持久化的 OSS 对象标识。 */ String objectRef) {
}
