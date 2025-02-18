// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.api.UserApi;
import com.digitalasset.quickstart.repository.TenantPropertiesRepository;
import com.digitalasset.quickstart.repository.TenantPropertiesRepository.TenantProperties;
import org.openapitools.model.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class UserApiImpl implements UserApi {

    @Autowired
    private TenantPropertiesRepository tenantPropertiesRepository;

    @Override
    public CompletableFuture<ResponseEntity<AuthenticatedUser>> getAuthenticatedUser() {
        OAuth2AuthenticationToken auth = null;
        if (SecurityContextHolder.getContext().getAuthentication() instanceof OAuth2AuthenticationToken) {
            auth = (OAuth2AuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        }
        if (auth == null || !auth.isAuthenticated()) {
            return CompletableFuture.completedFuture(ResponseEntity.status(401).build());
        }

        // Extract user and role info
        String party = auth.getPrincipal().getName();
        List<String> authorities = auth.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

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

        // Return the AuthenticatedUser in the response
        return CompletableFuture.completedFuture(ResponseEntity.ok(user));
    }
}
