// Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.api.LoginLinksApi;
import com.digitalasset.quickstart.security.oauth2.AuthClientRegistrationRepository;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.openapitools.model.LoginLink;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Controller
@RequestMapping("${openapi.asset.base-path:}")
@Profile("oauth2")
public class LoginLinksApiImpl implements LoginLinksApi {

    private final AuthClientRegistrationRepository clientRegistrationRepository;

    public LoginLinksApiImpl(AuthClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<List<LoginLink>>> listLinks() {
        return CompletableFuture.supplyAsync(() -> {
                    List<LoginLink> links = clientRegistrationRepository.getClientRegistrations().stream()
                            .map(registration ->
                                    new LoginLink()
                                            .name(registration.getTenantId())
                                            .url(clientRegistrationRepository.getLoginLink(registration.getRegistrationId()))
                            )
                            .collect(Collectors.toList());

                    return ResponseEntity.ok(links);
                }
        );
    }
}
