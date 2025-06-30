// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.api.AdminApi;
import com.digitalasset.quickstart.repository.TenantPropertiesRepository;
import com.digitalasset.quickstart.security.Auth;
import com.digitalasset.quickstart.security.AuthClientRegistrationRepository;
import com.digitalasset.quickstart.security.AuthClientRegistrationRepository.Client;
import com.digitalasset.quickstart.utility.LoggingSpanHelper;

import org.openapitools.model.TenantRegistration;
import org.openapitools.model.TenantRegistrationRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.digitalasset.quickstart.utility.ContextAwareCompletableFutures.completeWithin;
import static com.digitalasset.quickstart.utility.ContextAwareCompletableFutures.supplyWithin;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class AdminApiImpl implements AdminApi {

    private static final Logger logger = LoggerFactory.getLogger(AdminApiImpl.class);


    private final AuthClientRegistrationRepository authClientRegistrationRepository;
    private final TenantPropertiesRepository tenantPropertiesRepository;
    private final Auth auth;
    private final Optional<UserDetailsManager> userDetailsManager;

    @Autowired
    public AdminApiImpl(
            Optional<AuthClientRegistrationRepository> authClientRegistrationRepository,
            Optional<UserDetailsManager> userDetailsManager,
            TenantPropertiesRepository tenantPropertiesRepository,
            Auth auth
    ) {
        if (auth == Auth.OAUTH2 && authClientRegistrationRepository.isEmpty()) {
            throw new IllegalStateException("OAuth2 authentication is enabled but AuthClientRegistrationRepository is not configured");
        } else if (auth == Auth.SHARED_SECRET && userDetailsManager.isEmpty()) {
            throw new IllegalStateException("Shared secret authentication is enabled but UserDetailsManager is not configured");
        }
        this.auth = auth;
        this.authClientRegistrationRepository = authClientRegistrationRepository.orElse(null);
        this.userDetailsManager = userDetailsManager;
        this.tenantPropertiesRepository = tenantPropertiesRepository;
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<TenantRegistration>> createTenantRegistration(
            @SpanAttribute("tenant.request") TenantRegistrationRequest request
    ) {
        Span methodSpan = Span.current();
        Context parentContext = Context.current();

        Map<String, Object> commonAttrs = Map.of(
                "tenant.tenantId", request.getTenantId(),
                "tenant.clientId", request.getClientId(),
                "tenant.party", request.getPartyId()
        );

        LoggingSpanHelper.setSpanAttributes(methodSpan, commonAttrs);
        LoggingSpanHelper.logInfo(logger, "createTenantRegistration: Starting async creation", commonAttrs);

        return CompletableFuture
                .supplyAsync(
                        supplyWithin(parentContext, () -> {
                            LoggingSpanHelper.addEventWithAttributes(
                                    methodSpan,
                                    "Executing asynchronous logic for createTenantRegistration",
                                    null
                            );

                            if (auth == Auth.OAUTH2) {
                                Client c = new Client();
                                c.setTenantId(request.getTenantId());
                                c.setClientId(request.getClientId());
                                c.setIssuerURL(request.getIssuerUrl());
                                authClientRegistrationRepository.registerClient(c);
                            } else {
                                request.getUsers().forEach(user -> {
                                            logger.info(
                                                    "Creating user {} with roles {}",
                                                    user,
                                                    request.getInternal() ? "ADMIN" : "USER"
                                            );
                                            try {
                                                userDetailsManager.get().createUser(
                                                        org.springframework.security.core.userdetails.User
                                                                .withUsername(user)
                                                                .password("{noop}")
                                                                .roles(request.getInternal() ? "ADMIN" : "USER")
                                                                .build()
                                                );
                                            } catch (Exception e) {
                                                logger.error("Error creating user {}: {}", user, e.getMessage());
                                                throw e;
                                            }
                                        }
                                );
                            }

                            // Save extra properties in a separate repository
                            TenantPropertiesRepository.TenantProperties props = new TenantPropertiesRepository.TenantProperties();
                            props.setWalletUrl(request.getWalletUrl());
                            props.setPartyId(request.getPartyId());
                            props.setTenantId(request.getTenantId());
                            props.setInternal(request.getInternal());
                            props.setUsers(request.getUsers());
                            tenantPropertiesRepository.addTenant(request.getTenantId(), props);

                            // Build the response (OpenAPI model)
                            TenantRegistration response = new TenantRegistration();
                            response.setTenantId(request.getTenantId());
                            response.setPartyId(request.getPartyId());
                            response.setInternal(request.getInternal());
                            response.setClientId(request.getClientId());
                            if (request.getIssuerUrl() != null) {
                                response.setIssuerUrl(URI.create(request.getIssuerUrl()));
                            }
                            response.setIssuerUrl(URI.create(request.getIssuerUrl()));
                            response.setWalletUrl(URI.create(props.getWalletUrl()));
                            response.setUsers(request.getUsers());
                            return ResponseEntity.ok(response);
                        })
                )
                .whenComplete(
                        completeWithin(parentContext, (res, ex) -> {
                            if (ex == null) {
                                LoggingSpanHelper.logInfo(
                                        logger,
                                        "createTenantRegistration: Successfully created tenant registration",
                                        commonAttrs
                                );
                            } else {
                                LoggingSpanHelper.logError(
                                        logger,
                                        "createTenantRegistration: Failed",
                                        commonAttrs,
                                        ex
                                );
                                LoggingSpanHelper.recordException(methodSpan, ex);
                            }
                        })
                );
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<Void>> deleteTenantRegistration(
            @SpanAttribute("tenant.tenantId") String tenantId
    ) {
        Span methodSpan = Span.current();
        Context parentContext = Context.current();

        Map<String, Object> commonAttrs = Map.of("tenant.tenantId", tenantId);
        LoggingSpanHelper.setSpanAttributes(methodSpan, commonAttrs);
        LoggingSpanHelper.logInfo(logger, "deleteTenantRegistration: Starting async deletion", commonAttrs);

        return CompletableFuture
                .supplyAsync(
                        supplyWithin(parentContext, () -> {
                            LoggingSpanHelper.addEventWithAttributes(
                                    methodSpan,
                                    "Executing asynchronous logic for deleteTenantRegistration",
                                    null
                            );
                            if (auth == Auth.OAUTH2) {
                                authClientRegistrationRepository.removeClientRegistrations(tenantId);
                            } else {
                                tenantPropertiesRepository.getTenant(tenantId).getUsers()
                                        .forEach(userDetailsManager.get()::deleteUser);
                            }

                            tenantPropertiesRepository.removeTenant(tenantId);
                            return ResponseEntity.ok().<Void>build();
                        })
                )
                .whenComplete(
                        completeWithin(parentContext, (res, ex) -> {
                            if (ex == null) {
                                LoggingSpanHelper.logDebug(
                                        logger,
                                        "deleteTenantRegistration: Successfully deleted tenant registration",
                                        commonAttrs
                                );
                            } else {
                                LoggingSpanHelper.logError(
                                        logger,
                                        "deleteTenantRegistration: Failed",
                                        commonAttrs,
                                        ex
                                );
                                LoggingSpanHelper.recordException(methodSpan, ex);
                            }
                        })
                );
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<List<TenantRegistration>>> listTenantRegistrations() {
        Span methodSpan = Span.current();
        Context parentContext = Context.current();

        LoggingSpanHelper.logDebug(logger, "listTenantRegistrations: Starting async retrieval");

        return CompletableFuture
                .supplyAsync(
                        supplyWithin(parentContext, () -> {
                            LoggingSpanHelper.addEventWithAttributes(
                                    methodSpan,
                                    "Executing asynchronous logic for listTenantRegistrations",
                                    null
                            );


                    List<TenantRegistration> result;
                    if (auth == Auth.OAUTH2) {
                        result = authClientRegistrationRepository.getClientRegistrations().stream()
                                .map(c -> {
                                    TenantRegistration out = new TenantRegistration();
                                    out.setTenantId(c.getTenantId());
                                    out.setClientId(c.getClientId());
                                    out.setIssuerUrl(URI.create(c.getIssuerURL()));

                                    TenantPropertiesRepository.TenantProperties props = tenantPropertiesRepository.getTenant(c.getTenantId());
                                    if (props != null) {
                                        if (props.getWalletUrl() != null) out.setWalletUrl(URI.create(props.getWalletUrl()));
                                        out.setPartyId(props.getPartyId());
                                        out.setInternal(props.isInternal());
                                    }
                                    return out;
                                })
                                .collect(Collectors.toList());
                    } else {
                        result = tenantPropertiesRepository.getAllTenants().values().stream()
                                .map(prop -> {
                                    TenantRegistration out = new TenantRegistration();
                                    out.setTenantId(prop.getTenantId());
                                    if (prop.getWalletUrl() != null) out.setWalletUrl(URI.create(prop.getWalletUrl()));
                                    out.setPartyId(prop.getPartyId());
                                    out.setInternal(prop.isInternal());
                                    out.setUsers(prop.getUsers());
                                    return out;
                                })
                                .collect(Collectors.toList());
                    }

                    return ResponseEntity.ok(result);
                        })
                )
                .whenComplete(
                        completeWithin(parentContext, (res, ex) -> {
                            if (ex == null) {
                                Map<String, Object> successAttrs = Map.of(
                                        "list.count", res.getBody() != null ? res.getBody().size() : 0
                                );
                                LoggingSpanHelper.logDebug(logger, "listTenantRegistrations: Success", successAttrs);
                            } else {
                                LoggingSpanHelper.logError(
                                        logger,
                                        "listTenantRegistrations: Failed to list tenant registrations",
                                        null,
                                        ex
                                );
                                LoggingSpanHelper.recordException(methodSpan, ex);
                            }
                        })
                );
    }
}
