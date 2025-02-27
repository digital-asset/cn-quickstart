// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.api.AdminApi;
import com.digitalasset.quickstart.repository.TenantPropertiesRepository;
import com.digitalasset.quickstart.oauth.AuthClientRegistrationRepository;
import com.digitalasset.quickstart.oauth.AuthClientRegistrationRepository.Client;

// Updated models from the renamed OpenAPI spec
import org.openapitools.model.TenantRegistration;
import org.openapitools.model.TenantRegistrationRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class AdminApiImpl implements AdminApi {

    private final AuthClientRegistrationRepository authClientRegistrationRepository;
    private final TenantPropertiesRepository tenantPropertiesRepository;

    @Autowired
    public AdminApiImpl(
            AuthClientRegistrationRepository authClientRegistrationRepository,
            TenantPropertiesRepository tenantPropertiesRepository
    ) {
        this.authClientRegistrationRepository = authClientRegistrationRepository;
        this.tenantPropertiesRepository = tenantPropertiesRepository;
    }

    @Override
    public CompletableFuture<ResponseEntity<TenantRegistration>> createTenantRegistration(
            TenantRegistrationRequest request
    ) {
        Client c = new Client();
        c.setTenantId(request.getTenantId());
        c.setClientId(request.getClientId());
        c.setIssuerURL(request.getIssuerUrl());

        authClientRegistrationRepository.registerClient(c);

        // Save extra properties in a separate repository
        TenantPropertiesRepository.TenantProperties props = new TenantPropertiesRepository.TenantProperties();
        props.setWalletUrl(request.getWalletUrl());
        props.setPartyId(request.getPartyId());
        props.setTenantId(request.getTenantId());
        props.setInternal(request.getInternal());
        tenantPropertiesRepository.addTenant(request.getTenantId(), props);

        // Build the response (OpenAPI model)
        TenantRegistration response = new TenantRegistration();
        response.setTenantId(request.getTenantId());
        response.setPartyId(request.getPartyId());
        response.setInternal(request.getInternal());
        response.setClientId(request.getClientId());
        response.setIssuerUrl(URI.create(request.getIssuerUrl()));
        response.setWalletUrl(URI.create(props.getWalletUrl()));

        return CompletableFuture.completedFuture(ResponseEntity.ok(response));
    }

    @Override
    public CompletableFuture<ResponseEntity<Void>> deleteTenantRegistration(String tenantId) {
        authClientRegistrationRepository.removeClientRegistrations(tenantId);
        tenantPropertiesRepository.removeTenant(tenantId);
        return CompletableFuture.completedFuture(ResponseEntity.ok().build());
    }

    @Override
    public CompletableFuture<ResponseEntity<List<TenantRegistration>>> listTenantRegistrations() {
        List<TenantRegistration> result = authClientRegistrationRepository.getClientRegistrations().stream()
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

        return CompletableFuture.completedFuture(ResponseEntity.ok(result));
    }
}
