package com.digitalasset.quickstart.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthConfig {
    private AuthenticatedPartyProvider authenticatedPartyProvider;

    public AuthConfig(AuthenticatedPartyProvider authenticatedPartyProvider) {
        this.authenticatedPartyProvider = authenticatedPartyProvider;
    }

    @Bean
    public AuthUtils authUtils() {
        return new AuthUtils(authenticatedPartyProvider);
    }
}
