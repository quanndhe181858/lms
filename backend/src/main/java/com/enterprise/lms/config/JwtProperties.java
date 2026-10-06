package com.enterprise.lms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class JwtProperties {
    /** HMAC-SHA256 signing secret — must be ≥256 bits in production */
    private String secret;
    /** Access token lifetime in milliseconds (default 30 min) */
    private long accessTokenExpiryMs = 1_800_000L;
    /** Refresh token lifetime in milliseconds (default 7 days) */
    private long refreshTokenExpiryMs = 604_800_000L;
}
