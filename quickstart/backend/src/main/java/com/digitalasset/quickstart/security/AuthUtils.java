package com.digitalasset.quickstart.security;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class AuthUtils {
    private final AuthenticatedPartyProvider authenticatedPartyProvider;
    private final Auth auth;
    private static final Logger logger = LoggerFactory.getLogger(AuthUtils.class);

    @Value("${application.tenants.AppProvider.partyId}")
    private String appProviderPartyId;

    AuthUtils(AuthenticatedPartyProvider authenticatedPartyProvider, Auth auth) {
        this.authenticatedPartyProvider = authenticatedPartyProvider;
        this.auth = auth;
    }

    public String getAppProviderPartyId() {
        return appProviderPartyId;
    }

    public <T> CompletableFuture<ResponseEntity<T>> asAdminParty(Function<String, CompletableFuture<ResponseEntity<T>>> cf) {
        return asAuthenticatedParty(party -> {
            if (party.equals(appProviderPartyId)) {
                return cf.apply(party);
            } else {
                logger.error("Access denied: authenticated party '{}' does not match AppProvider party '{}'.", party, appProviderPartyId);
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
            }
        });
    }

    public <T> CompletableFuture<ResponseEntity<T>> asAuthenticatedParty(Function<String, CompletableFuture<ResponseEntity<T>>> cf) {
        return authenticatedPartyProvider.getParty().map(cf).orElseGet(() -> {
            logger.error("Authentication failed: no authenticated party present in the security context");
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        });
    }

    public boolean isOAuth2Enabled() {
        return auth ==  Auth.OAUTH2;
    }

    public boolean isSharedSecretEnabled() {
        return auth ==  Auth.SHARED_SECRET;
    }
}
