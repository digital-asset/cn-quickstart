// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.api.UserApi;
import com.digitalasset.quickstart.repository.TenantPropertiesRepository;
import com.digitalasset.quickstart.repository.TenantPropertiesRepository.TenantProperties;
import com.digitalasset.quickstart.utility.ContextAwareCompletableFutures;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.openapitools.model.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.digitalasset.quickstart.utility.ContextAwareCompletableFutures.completeWithin;
import static com.digitalasset.quickstart.utility.ContextAwareCompletableFutures.supplyWithin;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class UserApiImpl implements UserApi {

    private static final Logger logger = LoggerFactory.getLogger(UserApiImpl.class);

    @Autowired
    private TenantPropertiesRepository tenantPropertiesRepository;

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<AuthenticatedUser>> getAuthenticatedUser() {
        // Capture the current span and context
        Span methodSpan = Span.current();
        Context parentContext = Context.current();

        // Initial logging or event outside the future computation
        methodSpan.addEvent("Starting getAuthenticatedUser");
        logger.info("getAuthenticatedUser: invoked");
        SecurityContext securityContext = SecurityContextHolder.getContext();

        // Perform the logic asynchronously within the captured context
        return CompletableFuture
                .supplyAsync(
                        supplyWithin(parentContext, () -> {
                            methodSpan.addEvent("Performing authentication checks");

                            // Retrieve OAuth2 token from SecurityContext
                            OAuth2AuthenticationToken auth = null;
                            if (securityContext.getAuthentication() instanceof OAuth2AuthenticationToken) {
                                auth = (OAuth2AuthenticationToken) securityContext.getAuthentication();
                            }

                            // If no valid auth, throw exception (which we'll handle in exceptionally(...) below)
                            if (auth == null || !auth.isAuthenticated()) {
                                methodSpan.addEvent("User not authenticated");
                                throw new SecurityException("User is not authenticated");
                            }

                            // Extract user and role info
                            String party = auth.getPrincipal().getName();
                            List<String> authorities = auth.getAuthorities()
                                    .stream()
                                    .map(GrantedAuthority::getAuthority)
                                    .toList();

                            // Set a useful span attribute for debugging
                            methodSpan.setAttribute("authenticated.party", party);

                            // Retrieve the registrationId from the authentication
                            String registrationId = auth.getAuthorizedClientRegistrationId();

                            // Lookup wallet URL from tenant properties
                            String walletUrl = null;
                            TenantProperties props = tenantPropertiesRepository.getTenant(registrationId);
                            if (props != null && props.getWalletUrl() != null) {
                                walletUrl = props.getWalletUrl();
                            }

                            // Create the AuthenticatedUser object
                            AuthenticatedUser user = new AuthenticatedUser(
                                    // name
                                    party.split("::")[0],
                                    // party
                                    party,
                                    // roles
                                    authorities,
                                    // isAdmin
                                    authorities.contains("ROLE_ADMIN"),
                                    // walletUrl
                                    walletUrl
                            );

                            methodSpan.addEvent("Built AuthenticatedUser object, returning 200 OK");
                            return ResponseEntity.ok(user);
                        })
                )
                .whenComplete(
                        completeWithin(parentContext, (response, ex) -> {
                            if (ex == null) {
                                logger.info("getAuthenticatedUser: success");
                            } else {
                                logger.error("getAuthenticatedUser: error - {}", ex.getMessage(), ex);
                                methodSpan.recordException(ex);
                                methodSpan.setStatus(StatusCode.ERROR, ex.getMessage());
                            }
                        })
                )
                .exceptionally(ex -> {
                    // Convert certain exceptions to specific HTTP statuses
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof SecurityException) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                    }
                    // Fallback to 500 for other errors
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }
}
