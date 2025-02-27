// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.oauth.AuthClientRegistrationRepository;
import org.openapitools.model.LoginLink;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class LoginLinksApiImpl implements com.digitalasset.quickstart.api.LoginLinksApi {

    private final AuthClientRegistrationRepository clientRegistrationRepository;

    public LoginLinksApiImpl(AuthClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Override
    public CompletableFuture<ResponseEntity<List<LoginLink>>> listLinks() {

        // TODO when we implement proper login screen based on email of onboarded user
        List<LoginLink> links = clientRegistrationRepository.getClientRegistrations().stream()
                .map(registration ->
                        new LoginLink()
                                .name(registration.getTenantId())
                                .url(clientRegistrationRepository.getLoginLink(registration.getRegistrationId()))
                )
                .collect(Collectors.toList());

        return CompletableFuture.completedFuture(ResponseEntity.ok(links));
    }
}
