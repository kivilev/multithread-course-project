package dev.sorokin.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "clients.stub")
public class StubClientProperties {
    private String baseUrl;
    private Duration connectTimeout;
    private Duration readTimeout;
}
