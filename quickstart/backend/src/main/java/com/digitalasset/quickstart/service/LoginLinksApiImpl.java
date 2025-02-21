// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.api.LoginLinksApi;
import com.digitalasset.quickstart.repository.OAuth2ClientRegistrationRepository;
import com.digitalasset.quickstart.utility.ContextAwareCompletableFutures;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.openapitools.model.LoginLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.digitalasset.quickstart.utility.ContextAwareCompletableFutures.completeWithin;
import static com.digitalasset.quickstart.utility.ContextAwareCompletableFutures.supplyWithin;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class LoginLinksApiImpl implements LoginLinksApi {

    private static final Logger logger = LoggerFactory.getLogger(LoginLinksApiImpl.class);

    private final OAuth2ClientRegistrationRepository clientRegistrationRepository;

    public LoginLinksApiImpl(OAuth2ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<List<LoginLink>>> listLinks() {
        Span methodSpan = Span.current();
        Context parentContext = Context.current();

        methodSpan.addEvent("Starting listLinks");
        logger.info("listLinks: Retrieving login links asynchronously");

        return CompletableFuture
                .supplyAsync(
                        supplyWithin(parentContext, () -> {
                            methodSpan.addEvent("Building list of LoginLink objects from client registrations");

                            List<LoginLink> links = clientRegistrationRepository.getRegistrations().stream()
                                    .filter(registration -> AuthorizationGrantType.AUTHORIZATION_CODE.equals(registration.getAuthorizationGrantType()))
                                    .map(registration ->
                                            new LoginLink()
                                                    .name(registration.getClientName().split("::")[0])
                                                    .url("/oauth2/authorization/" + registration.getRegistrationId())
                                    )
                                    .collect(Collectors.toList());

                            return ResponseEntity.ok(links);
                        })
                )
                .whenComplete(
                        completeWithin(parentContext, (response, throwable) -> {
                            if (throwable == null) {
                                logger.info("listLinks: Completed successfully, found {} items",
                                        (response.getBody() != null ? response.getBody().size() : 0));
                            } else {
                                logger.error("listLinks: Failed with error: {}", throwable.getMessage(), throwable);
                                methodSpan.recordException(throwable);
                                methodSpan.setStatus(StatusCode.ERROR, throwable.getMessage());
                            }
                        })
                );
    }
}
