package com.digitalasset.quickstart.security;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;

// This class is a placeholder for utility methods related to authentication.
public class AuthUtils {
    private final AuthenticatedPartyProvider authenticatedPartyProvider;

    @Value("${application.tenants.AppProvider.partyId}")
    private String appProviderPartyId;

    AuthUtils(AuthenticatedPartyProvider authenticatedPartyProvider) {
        // Prevent instantiation
        this.authenticatedPartyProvider = authenticatedPartyProvider;
    }

    public String getAppProviderPartyId() {
        return appProviderPartyId;
    }

    public <T> CompletableFuture<T> asAdminParty(Function<String, CompletableFuture<T>> future) {
        var authParty = authenticatedPartyProvider.getParty();
        if (authParty.isPresent() && authParty.get().equals(appProviderPartyId))
            return CompletableFuture.completedFuture(appProviderPartyId).thenCompose(future);
        else
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "Authenticated party is not the AppProvider party: " + authParty.orElse("None")));
    }

    public <T> CompletableFuture<T> asAuthenticatedParty(Function<String, CompletableFuture<T>> future) {
        var authParty = authenticatedPartyProvider.getParty();
        return authParty.map(s -> CompletableFuture.completedFuture(s).thenCompose(future))
                .orElseGet(() -> CompletableFuture.failedFuture(new IllegalStateException(
                        "Authenticated party is not the AppProvider party: " + authParty.orElse("None"))));
    }
}
