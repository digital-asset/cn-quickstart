// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.api.UserApi;
import com.digitalasset.quickstart.oauth.OAuth2ClientRegistrationRepository;
import org.openapitools.model.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class UserApiImpl implements UserApi {

    @Autowired
    private OAuth2ClientRegistrationRepository clientRegistrationRepository;

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

        // Retrieve the ClientRegistration to find the walletUrl
        String registrationId = auth.getAuthorizedClientRegistrationId();
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(registrationId);

        String walletUrl = "http://localhost/"; // fallback if not found
        if (registration != null) {
            Map<String, Object> providerMetadata = registration
                    .getProviderDetails()
                    .getConfigurationMetadata();

            if (providerMetadata.containsKey("walletUrl")) {
                walletUrl = (String) providerMetadata.get("walletUrl");
            }
        }

        // Create the AuthenticatedUser object
        AuthenticatedUser user = new AuthenticatedUser(
                party.split("::")[0],    // name
                party,                         // party
                authorities,                   // roles
                authorities.contains("ROLE_ADMIN"), // isAdmin
                walletUrl                      // walletUrl
        );

        // Return the AuthenticatedUser in the response
        return CompletableFuture.completedFuture(ResponseEntity.ok(user));
    }
}
