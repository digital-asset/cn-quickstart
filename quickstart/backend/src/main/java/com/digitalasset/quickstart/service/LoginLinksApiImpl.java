// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.repository.OAuth2ClientRegistrationRepository;
import org.openapitools.model.LoginLink;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class LoginLinksApiImpl implements com.digitalasset.quickstart.api.LoginLinksApi {

    private final OAuth2ClientRegistrationRepository clientRegistrationRepository;

    public LoginLinksApiImpl(OAuth2ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Override
    public CompletableFuture<ResponseEntity<List<LoginLink>>> listLinks() {
        List<LoginLink> links = clientRegistrationRepository.getRegistrations().stream()
                .filter(registration -> AuthorizationGrantType.AUTHORIZATION_CODE.equals(registration.getAuthorizationGrantType()))
                .map(registration ->
                        new LoginLink()
                                .name(registration.getClientName().split("::")[0])
                                .url("/oauth2/authorization/" + registration.getRegistrationId()))
                .collect(Collectors.toList());

        return CompletableFuture.completedFuture(ResponseEntity.ok(links));
    }
}
