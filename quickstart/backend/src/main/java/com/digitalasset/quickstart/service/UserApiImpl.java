// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.api.UserApi;
import com.digitalasset.quickstart.security.AuthenticatedUserProvider;
import com.digitalasset.quickstart.repository.TenantPropertiesRepository;
import com.digitalasset.quickstart.repository.TenantPropertiesRepository.TenantProperties;
import org.openapitools.model.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class UserApiImpl implements UserApi {

    @Autowired
    private TenantPropertiesRepository tenantPropertiesRepository;
    @Autowired
    private AuthenticatedUserProvider authenticatedUserProvider;

    @Override
    public CompletableFuture<ResponseEntity<AuthenticatedUser>> getAuthenticatedUser() {
        return authenticatedUserProvider.getUser()
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

                    // Return the AuthenticatedUser in the response
                    return ResponseEntity.ok(out);
                })
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> CompletableFuture.completedFuture(ResponseEntity.status(401).build()));
    }
}
