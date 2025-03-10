package com.digitalasset.quickstart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security")
public class SecurityConfig {

    private String issuerUrl;

    public String getIssuerUrl() {
        return issuerUrl;
    }

    public void setIssuerUrl(String issuerUrl) {
        this.issuerUrl = issuerUrl;
    }
}
