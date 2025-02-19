// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.api.AdminApi;
import com.digitalasset.quickstart.repository.OAuth2ClientRegistrationRepository;
import com.digitalasset.quickstart.repository.TenantPropertiesRepository;
import com.digitalasset.quickstart.utility.LoggingSpanHelper;

import org.openapitools.model.TenantRegistration;
import org.openapitools.model.TenantRegistrationRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
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

    private final OAuth2ClientRegistrationRepository tenantRegistrationRepository;
    private final TenantPropertiesRepository tenantPropertiesRepository;

    @Autowired
    public AdminApiImpl(
            OAuth2ClientRegistrationRepository tenantRegistrationRepository,
            TenantPropertiesRepository tenantPropertiesRepository
    ) {
        this.tenantRegistrationRepository = tenantRegistrationRepository;
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
                "tenant.clientId", request.getClientId(),
                "tenant.party", request.getParty()
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

                            var registration = org.springframework.security.oauth2.client.registration.ClientRegistration
                                    .withRegistrationId(request.getClientId())
                                    .clientId(request.getClientId())
                                    .clientSecret(request.getClientSecret())
                                    .authorizationUri(request.getAuthorizationUri())
                                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                                    .tokenUri(request.getTokenUri())
                                    .jwkSetUri(request.getJwkSetUri())
                                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                                    .scope(request.getScope())
                                    .clientName(request.getParty())
                                    .providerConfigurationMetadata(Map.of("preconfigured", "false"))
                                    .build();

                            tenantRegistrationRepository.addRegistration(registration);

                            TenantPropertiesRepository.TenantProperties props = new TenantPropertiesRepository.TenantProperties();
                            props.setWalletUrl(request.getWalletUrl());
                            tenantPropertiesRepository.addTenant(registration.getRegistrationId(), props);

                            TenantRegistration response = new TenantRegistration();
                            response.setClientId(registration.getClientId());
                            response.setClientSecret(registration.getClientSecret());
                            response.setScope(String.join(" ", registration.getScopes()));
                            response.setAuthorizationUri(URI.create(registration.getProviderDetails().getAuthorizationUri()));
                            response.setTokenUri(URI.create(registration.getProviderDetails().getTokenUri()));
                            response.setJwkSetUri(URI.create(registration.getProviderDetails().getJwkSetUri()));
                            response.setParty(registration.getClientName());
                            response.setPreconfigured(false);
                            response.setWalletUrl(URI.create(props.getWalletUrl()));

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
                            tenantRegistrationRepository.removeRegistration(tenantId);
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

                            List<TenantRegistration> result = tenantRegistrationRepository
                                    .getRegistrations().stream()
                                    .filter(r -> AuthorizationGrantType.AUTHORIZATION_CODE.equals(r.getAuthorizationGrantType()))
                                    .map(r -> {
                                        TenantRegistration out = new TenantRegistration();
                                        out.setClientId(r.getClientId());
                                        out.setClientSecret(r.getClientSecret());
                                        out.setScope(String.join(" ", r.getScopes()));
                                        out.setAuthorizationUri(URI.create(r.getProviderDetails().getAuthorizationUri()));
                                        out.setTokenUri(URI.create(r.getProviderDetails().getTokenUri()));
                                        out.setJwkSetUri(URI.create(r.getProviderDetails().getJwkSetUri()));
                                        out.setParty(r.getClientName());
                                        Object preconfiguredFlag = r.getProviderDetails()
                                                .getConfigurationMetadata()
                                                .get("preconfigured");
                                        out.setPreconfigured("true".equals(preconfiguredFlag));
                                        TenantPropertiesRepository.TenantProperties props =
                                                tenantPropertiesRepository.getTenant(r.getRegistrationId());
                                        if (props != null && props.getWalletUrl() != null) {
                                            out.setWalletUrl(URI.create(props.getWalletUrl()));
                                        }
                                        return out;
                                    })
                                    .collect(Collectors.toList());

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
