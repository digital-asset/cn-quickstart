// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.oauth;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class AuthenticatedPartyService {

    public CompletableFuture<Optional<String>> getParty() {
        // Capture the SecurityContext right now
        SecurityContext context = SecurityContextHolder.getContext();
        // Return a CompletableFuture that we can compose with other async operations without having to worry about the SecurityContext
        return CompletableFuture.supplyAsync(() -> {
            if (!(context.getAuthentication() instanceof OAuth2AuthenticationToken)) {
                return Optional.empty();
            }
            OAuth2AuthenticationToken auth = (OAuth2AuthenticationToken) context.getAuthentication();
            if (!auth.isAuthenticated()) {
                return Optional.empty();
            }
            return Optional.of(auth.getPrincipal().getName());
        });
    }

    /**
     * Return the party name or fail with an IllegalStateException.
     */
    public CompletableFuture<String> getPartyOrFail() {
        return getParty().thenApply(opt ->
                opt.orElseThrow(() -> new IllegalStateException("No authenticated party"))
        );
    }
}
