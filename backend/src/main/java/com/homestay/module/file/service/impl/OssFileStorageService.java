package com.homestay.module.file.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.homestay.module.file.service.FileStorageService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

/** 将房型图片保存至私有 OSS，并生成临时查看地址。 */
@Service
public class OssFileStorageService implements FileStorageService {
    private static final String OBJECT_PREFIX = "rooms/";
    private static final String REFERENCE_PREFIX = "oss://";
    private static final Duration URL_LIFETIME = Duration.ofHours(1);
    private final ObjectProvider<OSS> client;
    private final String bucket;

    public OssFileStorageService(ObjectProvider<OSS> client, @Value("${aliyun.oss.bucket}") String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public String upload(InputStream input, String filename, String contentType) {
        // 图片对象使用独立名称，避免覆盖已经关联的文件。
        int dot = filename.lastIndexOf('.');
        String extension = dot < 0 ? "" : filename.substring(dot).toLowerCase();
        String key = OBJECT_PREFIX + UUID.randomUUID() + extension;
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        client.getObject().putObject(bucket, key, input, metadata);
        return REFERENCE_PREFIX + bucket + "/" + key;
    }

    @Override
    public String accessUrl(String reference) {
        String prefix = REFERENCE_PREFIX + bucket + "/";
        if (!reference.startsWith(prefix)) {
            return reference;
        }
        Date expiration = new Date(System.currentTimeMillis() + URL_LIFETIME.toMillis());
        return client.getObject().generatePresignedUrl(bucket, reference.substring(prefix.length()), expiration).toString();
    }
}
