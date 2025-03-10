// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD
package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.api.UserApi;
import com.digitalasset.quickstart.security.AuthenticatedUserProvider;
import com.digitalasset.quickstart.repository.TenantPropertiesRepository;
import com.digitalasset.quickstart.repository.TenantPropertiesRepository.TenantProperties;
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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.digitalasset.quickstart.utility.ContextAwareCompletableFutures.completeWithin;
import static com.digitalasset.quickstart.utility.ContextAwareCompletableFutures.supplyWithin;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class UserApiImpl implements UserApi {

    private static final Logger logger = LoggerFactory.getLogger(UserApiImpl.class);

    @Autowired
    private TenantPropertiesRepository tenantPropertiesRepository;
    @Autowired
    private AuthenticatedUserProvider authenticatedUserProvider;

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<AuthenticatedUser>> getAuthenticatedUser() {
        Span methodSpan = Span.current();
        Context parentContext = Context.current();

        methodSpan.addEvent("Starting getAuthenticatedUser");
        logger.atInfo().log("Received request, retrieving authenticated user asynchronously");

        return CompletableFuture.completedFuture(authenticatedUserProvider.getUser())
                .thenCompose(maybeUser ->
                    CompletableFuture.supplyAsync(
                        supplyWithin(parentContext, () -> {
                            methodSpan.addEvent("Performing authentication checks");
                            return maybeUser
                                    .map(user -> {
                                        // Lookup wallet URL from tenant properties
                                        String walletUrl = Optional.ofNullable(tenantPropertiesRepository.getTenant(user.getTenantId()))
                                                .map(TenantProperties::getWalletUrl)
                                                .orElse(null);

                                        // Create the AuthenticatedUser object
                                        AuthenticatedUser out = new AuthenticatedUser(
                                                user.getUsername(),
                                                user.getPartyId(),
                                                user.getRoles(),
                                                user.isAdmin(),
                                                walletUrl
                                        );
//
                                        Map<String, Object> userDetailsAttrs = Map.of(
                                                "authenticated.party", user.getPartyId(),
                                                "authorities", user.getRoles()
                                        );
                                        LoggingSpanHelper.setSpanAttributes(methodSpan, userDetailsAttrs);
                                        LoggingSpanHelper.logDebug(logger, "Resolved user details", userDetailsAttrs);

                                        LoggingSpanHelper.addEventWithAttributes(methodSpan, "Constructed AuthenticatedUser object (200 OK)", null);

                                        // Return the AuthenticatedUser in the response
                                        return ResponseEntity.ok(out);
                                    })
                                    .orElseThrow(() -> new SecurityException("User is not authenticated"));
                        })
                    )
                ).whenComplete(
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
                        methodSpan.addEvent("User not authenticated");
                        LoggingSpanHelper.logInfo(logger, "User is not authenticated");
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                    }
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }
}
