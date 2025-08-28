// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.api.AdminApi;
import com.digitalasset.quickstart.repository.TenantPropertiesRepository;
import com.digitalasset.quickstart.security.AuthClientRegistrationRepository;
import com.digitalasset.quickstart.security.AuthClientRegistrationRepository.Client;
import com.digitalasset.quickstart.security.AuthUtils;

import org.openapitools.model.TenantRegistration;
import org.openapitools.model.TenantRegistrationRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.opentelemetry.instrumentation.annotations.WithSpan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.digitalasset.quickstart.service.ServiceUtils.traceServiceCallAsync;
import static com.digitalasset.quickstart.utility.TracingUtils.tracingCtx;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class AdminApiImpl implements AdminApi {
    private static final Logger logger = LoggerFactory.getLogger(AdminApiImpl.class);
    private final AuthClientRegistrationRepository authClientRegistrationRepository;
    private final TenantPropertiesRepository tenantPropertiesRepository;
    private final Optional<UserDetailsManager> userDetailsManager;
    private final AuthUtils auth;

    @Autowired
    public AdminApiImpl(
            Optional<AuthClientRegistrationRepository> authClientRegistrationRepository,
            Optional<UserDetailsManager> userDetailsManager,
            TenantPropertiesRepository tenantPropertiesRepository,
            AuthUtils auth
    ) {
        this.auth = auth;
        if (auth.isOAuth2Enabled() && authClientRegistrationRepository.isEmpty()) {
            throw new IllegalStateException("OAuth2 authentication is enabled but AuthClientRegistrationRepository is not configured");
        } else if (auth.isSharedSecretEnabled() && userDetailsManager.isEmpty()) {
            throw new IllegalStateException("Shared secret authentication is enabled but UserDetailsManager is not configured");
        }
        this.authClientRegistrationRepository = authClientRegistrationRepository.orElse(null);
        this.userDetailsManager = userDetailsManager;
        this.tenantPropertiesRepository = tenantPropertiesRepository;
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<TenantRegistration>> createTenantRegistration(TenantRegistrationRequest request) {
        var ctx = tracingCtx(logger, "createTenantRegistration",
                "tenantId", request.getTenantId(),
                "clientId", request.getClientId(),
                "partyId", request.getPartyId()
        );

        // TODO KV fix
        //  We cannot use auth.asAdminParty(party -> here. if this endpoint is accessed from CLI providing JWT token directly
        //  The endpoint is still protected as SpringSecurityOAuth2Config -> HttpSecurity requires role ADMIN for it
        //  but it is annoying that we cannot use the same pattern as in other endpoints
        return traceServiceCallAsync(ctx, () -> CompletableFuture.supplyAsync(() -> {
            if (auth.isOAuth2Enabled()) {
                Client c = new Client();
                c.setTenantId(request.getTenantId());
                c.setClientId(request.getClientId());
                c.setIssuerURL(request.getIssuerUrl());
                authClientRegistrationRepository.registerClient(c);
            } else {
                request.getUsers().forEach(user -> {
                            logger.info("Creating user {} with roles {}", user, request.getInternal() ? "ADMIN" : "USER");
                            try {
                                userDetailsManager.get().createUser(
                                        // TODO KV https://github.com/digital-asset/cn-quickstart/issues/235
                                        //  fix this API leak, we should not rely on Spring Security here
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
        }));
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<Void>> deleteTenantRegistration(String tenantId) {
        var ctx = tracingCtx(logger, "deleteTenantRegistration", "tenantId", tenantId);
        return auth.asAdminParty(party -> traceServiceCallAsync(ctx, () -> CompletableFuture.supplyAsync(() -> {
            if (auth.isOAuth2Enabled()) {
                authClientRegistrationRepository.removeClientRegistrations(tenantId);
            } else {
                tenantPropertiesRepository.getTenant(tenantId).getUsers().forEach(userDetailsManager.get()::deleteUser);
            }

            tenantPropertiesRepository.removeTenant(tenantId);
            return ResponseEntity.ok().<Void>build();
        })));
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<List<TenantRegistration>>> listTenantRegistrations() {
        var ctx = tracingCtx(logger, "listTenantRegistrations");
        return auth.asAdminParty(party -> traceServiceCallAsync(ctx, () -> CompletableFuture.supplyAsync(() -> {
            List<TenantRegistration> result;
            if (auth.isOAuth2Enabled()) {
                result = authClientRegistrationRepository.getClientRegistrations().stream()
                        .map(c -> {
                            TenantRegistration out = new TenantRegistration();
                            out.setTenantId(c.getTenantId());
                            out.setClientId(c.getClientId());
                            out.setIssuerUrl(URI.create(c.getIssuerURL()));

                            TenantPropertiesRepository.TenantProperties props = tenantPropertiesRepository.getTenant(c.getTenantId());
                            if (props != null) {
                                if (props.getWalletUrl() != null)
                                    out.setWalletUrl(URI.create(props.getWalletUrl()));
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
                            if (prop.getWalletUrl() != null)
                                out.setWalletUrl(URI.create(prop.getWalletUrl()));
                            out.setPartyId(prop.getPartyId());
                            out.setInternal(prop.isInternal());
                            out.setUsers(prop.getUsers());
                            return out;
                        })
                        .collect(Collectors.toList());
            }
            return ResponseEntity.ok(result);
        })));
    }
}
