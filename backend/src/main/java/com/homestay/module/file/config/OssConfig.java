package com.homestay.module.file.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/** 文件模块的 OSS 客户端配置；凭证填写在后端配置文件中，按需初始化。 */
@Configuration
public class OssConfig {
    @Bean(destroyMethod = "shutdown")
    @Lazy
    public OSS ossClient(@Value("${aliyun.oss.endpoint}") String endpoint,
                         @Value("${aliyun.oss.access-key-id}") String accessKeyId,
                         @Value("${aliyun.oss.access-key-secret}") String accessKeySecret) {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}
