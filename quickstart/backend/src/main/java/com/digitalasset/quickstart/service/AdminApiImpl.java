// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.oauth.OAuth2ClientRegistrationRepository;
import org.openapitools.model.AppClientRegistration;
import org.openapitools.model.OAuth2ClientRegistrationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class AdminApiImpl implements com.digitalasset.quickstart.api.Oauth2Api {

    private final OAuth2ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    public AdminApiImpl(OAuth2ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Override
    public CompletableFuture<ResponseEntity<AppClientRegistration>> createClientRegistration(
            OAuth2ClientRegistrationRequest request
    ) {
        // Build the provider config metadata map
        Map<String, Object> providerMetadata = new HashMap<>();
        providerMetadata.put("preconfigured", "false");
        // Convert from URI to String directly
        providerMetadata.put("walletUrl", request.getWalletUrl());

        // Build the ClientRegistration
        ClientRegistration clientRegistration = ClientRegistration
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
                .providerConfigurationMetadata(providerMetadata)
                .build();

        // Store it in the repository
        clientRegistrationRepository.addRegistration(clientRegistration);

        // Build a return object
        // Convert back from String to URI using URI.create(...)
        AppClientRegistration result = new AppClientRegistration();
        result.setClientId(clientRegistration.getClientId());
        result.setClientSecret(clientRegistration.getClientSecret());
        result.setScope(String.join(" ", clientRegistration.getScopes()));
        result.setAuthorizationUri(URI.create(clientRegistration.getProviderDetails().getAuthorizationUri()));
        result.setTokenUri(URI.create(clientRegistration.getProviderDetails().getTokenUri()));
        result.setJwkSetUri(URI.create(clientRegistration.getProviderDetails().getJwkSetUri()));
        result.setParty(clientRegistration.getClientName());
        result.setPreconfigured(false);

        String storedWalletUrl = (String) clientRegistration
                .getProviderDetails()
                .getConfigurationMetadata()
                .get("walletUrl");
        result.setWalletUrl(URI.create(storedWalletUrl));

        return CompletableFuture.completedFuture(ResponseEntity.ok(result));
    }

    @Override
    public CompletableFuture<ResponseEntity<Void>> deleteClientRegistration(String clientId) {
        clientRegistrationRepository.removeRegistration(clientId);
        return CompletableFuture.completedFuture(ResponseEntity.ok().build());
    }

    @Override
    public CompletableFuture<ResponseEntity<List<AppClientRegistration>>> listClientRegistrations() {
        List<AppClientRegistration> registrations = clientRegistrationRepository.getRegistrations().stream()
                .filter(registration ->
                        AuthorizationGrantType.AUTHORIZATION_CODE.equals(registration.getAuthorizationGrantType()))
                .map(registration -> {
                    AppClientRegistration appClientRegistration = new AppClientRegistration();
                    appClientRegistration.setClientId(registration.getClientId());
                    appClientRegistration.setAuthorizationUri(
                            URI.create(registration.getProviderDetails().getAuthorizationUri())
                    );
                    appClientRegistration.setTokenUri(
                            URI.create(registration.getProviderDetails().getTokenUri())
                    );
                    appClientRegistration.setJwkSetUri(
                            URI.create(registration.getProviderDetails().getJwkSetUri())
                    );
                    appClientRegistration.setScope(String.join(" ", registration.getScopes()));
                    appClientRegistration.setParty(registration.getClientName());
                    appClientRegistration.setPreconfigured(
                            registration.getProviderDetails()
                                    .getConfigurationMetadata()
                                    .containsKey("preconfigured"));

                    String walletUrlStr = (String) registration.getProviderDetails()
                            .getConfigurationMetadata()
                            .get("walletUrl");
                    appClientRegistration.setWalletUrl(URI.create(walletUrlStr));

                    return appClientRegistration;
                })
                .collect(Collectors.toList());

        return CompletableFuture.completedFuture(ResponseEntity.ok(registrations));
    }
}
