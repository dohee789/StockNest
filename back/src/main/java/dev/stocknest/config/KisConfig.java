package dev.stocknest.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "kis")
public class KisConfig {
    private String appKey;
    private String appSecret;
    private String baseUrl;
}
