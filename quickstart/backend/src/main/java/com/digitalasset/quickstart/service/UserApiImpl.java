// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD
package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.api.UserApi;
import com.digitalasset.quickstart.repository.TenantPropertiesRepository;
import com.digitalasset.quickstart.repository.TenantPropertiesRepository.TenantProperties;
import com.digitalasset.quickstart.utility.ContextAwareCompletableFutures;
import com.digitalasset.quickstart.utility.LoggingSpanHelper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
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
import java.util.Map;
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
        Span methodSpan = Span.current();
        Context parentContext = Context.current();

        methodSpan.addEvent("Starting getAuthenticatedUser");
        logger.atInfo().log("Received request, retrieving authenticated user asynchronously");

        SecurityContext securityContext = SecurityContextHolder.getContext();

        return CompletableFuture
                .supplyAsync(
                        supplyWithin(parentContext, () -> {
                            methodSpan.addEvent("Performing authentication checks");

                            OAuth2AuthenticationToken auth = null;
                            if (securityContext.getAuthentication() instanceof OAuth2AuthenticationToken) {
                                auth = (OAuth2AuthenticationToken) securityContext.getAuthentication();
                            }

                            if (auth == null || !auth.isAuthenticated()) {
                                methodSpan.addEvent("User not authenticated");
                                LoggingSpanHelper.logInfo(logger, "User is not authenticated");
                                throw new SecurityException("User is not authenticated");
                            }

                            String party = auth.getPrincipal().getName();
                            List<String> authorities = auth.getAuthorities()
                                    .stream()
                                    .map(GrantedAuthority::getAuthority)
                                    .toList();

                            Map<String, Object> userDetailsAttrs = Map.of(
                                    "authenticated.party", party,
                                    "authorities", authorities
                            );
                            LoggingSpanHelper.setSpanAttributes(methodSpan, userDetailsAttrs);
                            LoggingSpanHelper.logDebug(logger, "Resolved user details", userDetailsAttrs);

                            String registrationId = auth.getAuthorizedClientRegistrationId();
                            String walletUrl = null;
                            TenantProperties props = tenantPropertiesRepository.getTenant(registrationId);
                            if (props != null && props.getWalletUrl() != null) {
                                walletUrl = props.getWalletUrl();
                            }

                            AuthenticatedUser user = new AuthenticatedUser(
                                    party.split("::")[0],
                                    party,
                                    authorities,
                                    authorities.contains("ROLE_ADMIN"),
                                    walletUrl
                            );

                            LoggingSpanHelper.addEventWithAttributes(methodSpan, "Constructed AuthenticatedUser object (200 OK)", null);
                            return ResponseEntity.ok(user);
                        })
                )
                .whenComplete(
                        completeWithin(parentContext, (response, ex) -> {
                            if (ex == null) {
                                logger.atInfo().log("Successfully retrieved authenticated user");
                            } else {
                                logger.atError().setCause(ex).log("Error retrieving authenticated user");
                                LoggingSpanHelper.recordException(methodSpan, ex);
                                methodSpan.setStatus(StatusCode.ERROR, ex.getMessage());
                            }
                        })
                )
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof SecurityException) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                    }
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }
}
