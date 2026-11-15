package com.homestay.config;

import com.homestay.common.constants.UploadConstants;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@Configuration
public class MultipartConfig {

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofBytes(UploadConstants.MAX_FILE_SIZE_BYTES));
        factory.setMaxRequestSize(DataSize.ofBytes(UploadConstants.MAX_REQUEST_SIZE_BYTES));
        return factory.createMultipartConfig();
    }
}
