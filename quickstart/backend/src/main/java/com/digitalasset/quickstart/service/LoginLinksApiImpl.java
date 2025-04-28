// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.api.LoginLinksApi;
import com.digitalasset.quickstart.security.AuthClientRegistrationRepository;
import com.digitalasset.quickstart.utility.ContextAwareCompletableFutures;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.openapitools.model.LoginLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.digitalasset.quickstart.utility.ContextAwareCompletableFutures.completeWithin;
import static com.digitalasset.quickstart.utility.ContextAwareCompletableFutures.supplyWithin;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
@Profile("oauth2")
public class LoginLinksApiImpl implements LoginLinksApi {

    private static final Logger logger = LoggerFactory.getLogger(LoginLinksApiImpl.class);

    private final AuthClientRegistrationRepository clientRegistrationRepository;

    // KV if authOn=false then do just simple login screen? simple link? oauth2=false? shouldn't that be done on the client side?
    //    problem with client is that it cannot read environment variables easily
    public LoginLinksApiImpl(AuthClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<List<LoginLink>>> listLinks() {
        Span methodSpan = Span.current();
        Context parentContext = Context.current();

        methodSpan.addEvent("Starting listLinks");
        logger.atInfo().log("listLinks: Received request, retrieving login links asynchronously");

        return CompletableFuture
                .supplyAsync(
                        supplyWithin(parentContext, () -> {
                            methodSpan.addEvent("Building list of LoginLink objects from client registrations");

                            List<LoginLink> links = clientRegistrationRepository.getClientRegistrations().stream()
                                    .map(registration ->
                                            new LoginLink()
                                                    .name(registration.getTenantId())
                                                    .url(clientRegistrationRepository.getLoginLink(registration.getRegistrationId()))
                                    )
                                    .collect(Collectors.toList());

                            return ResponseEntity.ok(links);
                        })
                )
                .whenComplete(
                        completeWithin(parentContext, (response, throwable) -> {
                            if (throwable == null) {
                                logger.atInfo()
                                        .addKeyValue("itemsFound", response.getBody() != null ? response.getBody().size() : 0)
                                        .log("listLinks: Completed successfully");
                            } else {
                                logger.atError()
                                        .setCause(throwable)
                                        .log("listLinks: Failed with error");
                                methodSpan.recordException(throwable);
                                methodSpan.setStatus(StatusCode.ERROR, throwable.getMessage());
                            }
                        })
                );
    }
}
