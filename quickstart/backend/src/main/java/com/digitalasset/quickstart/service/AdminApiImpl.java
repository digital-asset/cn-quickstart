// Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.api.AdminApi;
import com.digitalasset.quickstart.repository.TenantPropertiesRepository;
import com.digitalasset.quickstart.security.oauth2.AuthClientRegistrationRepository;
import com.digitalasset.quickstart.security.oauth2.AuthClientRegistrationRepository.Client;
import com.digitalasset.quickstart.security.AuthUtils;

import org.openapitools.model.TenantRegistration;
import org.openapitools.model.TenantRegistrationRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.opentelemetry.instrumentation.annotations.WithSpan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.digitalasset.quickstart.service.ServiceUtils.traceServiceCallAsync;
import static com.digitalasset.quickstart.utility.TracingUtils.tracingCtx;

import jakarta.validation.constraints.NotNull;

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

    private void validateRequest(@NotNull TenantRegistrationRequest request) {
        Function<String, ResponseStatusException> badRequestExc = msg -> new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
        if (request.getTenantId() == null || request.getTenantId().isBlank()) {
            throw badRequestExc.apply("Tenant Id is required");
        }
        if (request.getPartyId() == null || request.getPartyId().isBlank()) {
            throw badRequestExc.apply("Party Id is required");
        }
        if (auth.isOAuth2Enabled()) {
            if (request.getClientId() == null || request.getClientId().isBlank()) {
                throw badRequestExc.apply("Client Id is required in OAuth2 mode");
            }
            if (request.getIssuerUrl() == null || request.getIssuerUrl().isBlank()) {
                throw badRequestExc.apply("Issuer Url is required in OAuth2 mode");
            }
        } else if (auth.isSharedSecretEnabled()) {
            if (request.getUsers() == null || request.getUsers().isEmpty()) {
                throw badRequestExc.apply("At least one User is required in shared-secret mode");
            }
        }
    }

    private void ensureTenantIsUnique(TenantRegistrationRequest request) {
        Function<String, ResponseStatusException> conflictExc = msg -> new ResponseStatusException(HttpStatus.CONFLICT, msg);
        if (tenantPropertiesRepository.getTenant(request.getTenantId()) != null) {
            throw conflictExc.apply("TenantId already exists");
        }
        if (auth.isOAuth2Enabled()) {
            boolean clientIssuerCombinationExists = authClientRegistrationRepository.getClientRegistrations().stream()
              .anyMatch(c -> c.getClientId().equals(request.getClientId()) && c.getIssuerURL().equals(request.getIssuerUrl()));
            if (clientIssuerCombinationExists) {
                throw conflictExc.apply("ClientId-IssuerUrl combination already exists");
            }
        }
    }

    private void registerOAuthClient(TenantRegistrationRequest request) {
            Client c = new Client();
            c.setTenantId(request.getTenantId());
            c.setClientId(request.getClientId());
            c.setIssuerURL(request.getIssuerUrl());
            authClientRegistrationRepository.registerClient(c);
    }

    private void registerSharedSecretUsers(TenantRegistrationRequest request) {
        request.getUsers().forEach(user -> {
            logger.info("Creating user {} with roles {}", user, "USER");
            try {
                userDetailsManager.get().createUser(
                    // TODO KV https://github.com/digital-asset/cn-quickstart/issues/235
                    //  fix this API leak, we should not rely on Spring Security here
                    org.springframework.security.core.userdetails.User
                        .withUsername(user)
                        .password("{noop}")
                        .roles("USER")
                        .build()
                );
            } catch (Exception e) {
                logger.error("Error creating user {}: {}", user, e.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
            }
        });
    }

    private void persistTenantMetadata(TenantRegistrationRequest request) {
        TenantPropertiesRepository.TenantProperties props = new TenantPropertiesRepository.TenantProperties();
        props.setWalletUrl(request.getWalletUrl());
        props.setPartyId(request.getPartyId());
        props.setTenantId(request.getTenantId());
        props.setUsers(request.getUsers());
        tenantPropertiesRepository.addTenant(request.getTenantId(), props);
    }

    private TenantRegistration buildResponse(TenantRegistrationRequest request) {
        TenantRegistration response = new TenantRegistration();
        response.setTenantId(request.getTenantId());
        response.setPartyId(request.getPartyId());
        response.setClientId(request.getClientId());
        if (request.getIssuerUrl() != null && !request.getIssuerUrl().isBlank()) {
            response.setIssuerUrl(URI.create(request.getIssuerUrl()));
        }
        if (request.getWalletUrl() != null && !request.getWalletUrl().isBlank()) {
            response.setWalletUrl(URI.create(request.getWalletUrl()));
        }
        response.setUsers(request.getUsers());
        return response;
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<TenantRegistration>> createTenantRegistration(TenantRegistrationRequest request) {
        var ctx = tracingCtx(logger, "createTenantRegistration",
            "tenantId", request.getTenantId(),
            "clientId", request.getClientId(),
            "partyId", request.getPartyId()
        );
        return auth.asAdminParty(party -> traceServiceCallAsync(ctx, () -> CompletableFuture.supplyAsync(() -> {
            validateRequest(request);
            ensureTenantIsUnique(request);
            if (auth.isOAuth2Enabled()) {
                registerOAuthClient(request);
            } else {
                registerSharedSecretUsers(request);
            }
            // Save extra properties in a separate repository
            persistTenantMetadata(request);
            // Build the response (OpenAPI model)
            return ResponseEntity.status(HttpStatus.CREATED).body(buildResponse(request));
        })));
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<Void>> deleteTenantRegistration(String tenantId) {
        var ctx = tracingCtx(logger, "deleteTenantRegistration", "tenantId", tenantId);
        return auth.asAdminParty(party -> traceServiceCallAsync(ctx, () -> CompletableFuture.supplyAsync(() -> {
            try {
                if (auth.isOAuth2Enabled()) {
                    authClientRegistrationRepository.removeClientRegistrations(tenantId);
                } else {
                    tenantPropertiesRepository.getTenant(tenantId).getUsers().forEach(userDetailsManager.get()::deleteUser);
                }

                tenantPropertiesRepository.removeTenant(tenantId);
            } catch (NoSuchElementException e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
            }
            return ResponseEntity.status(HttpStatus.NO_CONTENT).<Void>build();
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
