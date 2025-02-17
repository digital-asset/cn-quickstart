// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.api.AdminApi;
import com.digitalasset.quickstart.repository.TenantPropertiesRepository;
import com.digitalasset.quickstart.repository.OAuth2ClientRegistrationRepository;

// Updated models from the renamed OpenAPI spec
import org.openapitools.model.TenantRegistration;
import org.openapitools.model.TenantRegistrationRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class AdminApiImpl implements AdminApi {

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
    public CompletableFuture<ResponseEntity<TenantRegistration>> createTenantRegistration(
            TenantRegistrationRequest request
    ) {
        // Build the Spring Security OAuth2 ClientRegistration
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
                // Mark as preconfigured=false, i.e. added at runtime
                .providerConfigurationMetadata(java.util.Map.of("preconfigured", "false"))
                .build();

        // Save the registration in your custom repository
        tenantRegistrationRepository.addRegistration(registration);

        // Save extra properties in a separate repository
        TenantPropertiesRepository.TenantProperties props = new TenantPropertiesRepository.TenantProperties();
        props.setWalletUrl(request.getWalletUrl());
        tenantPropertiesRepository.addTenant(registration.getRegistrationId(), props);

        // Build the response (OpenAPI model)
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

        return CompletableFuture.completedFuture(ResponseEntity.ok(response));
    }

    @Override
    public CompletableFuture<ResponseEntity<Void>> deleteTenantRegistration(String tenantId) {
        tenantRegistrationRepository.removeRegistration(tenantId);
        tenantPropertiesRepository.removeTenant(tenantId);
        return CompletableFuture.completedFuture(ResponseEntity.ok().build());
    }

    @Override
    public CompletableFuture<ResponseEntity<List<TenantRegistration>>> listTenantRegistrations() {
        List<TenantRegistration> result = tenantRegistrationRepository.getRegistrations().stream()
                // Filter only those using the AUTHORIZATION_CODE grant type
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

                    // Determine whether it was preconfigured or added at runtime
                    Object preconfiguredFlag = r.getProviderDetails()
                            .getConfigurationMetadata()
                            .get("preconfigured");
                    out.setPreconfigured("true".equals(preconfiguredFlag));

                    // Populate walletUrl from your separate repository
                    TenantPropertiesRepository.TenantProperties props =
                            tenantPropertiesRepository.getTenant(r.getRegistrationId());
                    if (props != null && props.getWalletUrl() != null) {
                        out.setWalletUrl(URI.create(props.getWalletUrl()));
                    }
                    return out;
                })
                .collect(Collectors.toList());

        return CompletableFuture.completedFuture(ResponseEntity.ok(result));
    }
}
