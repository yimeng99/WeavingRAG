package com.weaving.llm.common.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: 依梦
 * @Date: 2025/10/27
 * @Description: 阿里云 OSS 配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
public class OSSConfig {
    
    /**
     * OSS 访问端点 (endpoint)
     */
    private String endpoint;
    
    /**
     * 访问密钥 ID
     */
    private String accessKeyId;
    
    /**
     * 访问密钥密码
     */
    private String accessKeySecret;
    
    /**
     * Bucket 名称
     */
    private String bucketName;
    
    /**
     * 创建 OSSClient Bean
     */
    @Bean
    public OSS ossClient() {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}
