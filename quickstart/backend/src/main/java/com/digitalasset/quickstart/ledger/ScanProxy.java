// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.ledger;

import com.digitalasset.quickstart.validatorproxy.client.ApiException;
import com.digitalasset.quickstart.validatorproxy.client.api.ScanProxyApi;
import com.digitalasset.quickstart.validatorproxy.client.model.GetAmuletRulesProxyResponse;
import com.digitalasset.quickstart.validatorproxy.client.model.GetDsoPartyIdResponse;
import com.digitalasset.quickstart.validatorproxy.client.model.GetOpenAndIssuingMiningRoundsProxyResponse;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class ScanProxy {
    private final ScanProxyApi scanProxyApi;
    private final Logger logger = LoggerFactory.getLogger(ScanProxy.class);

    public ScanProxy(ScanProxyApi scanProxyApi) {
        this.scanProxyApi = scanProxyApi;
    }

    @WithSpan
    public CompletableFuture<GetDsoPartyIdResponse> getDsoPartyId() {
        Span currentSpan = Span.current();
        currentSpan.setAttribute("backend.apiName", "ScanProxy");
        currentSpan.setAttribute("backend.method", "getDsoPartyId");

        logger.debug("Fetching DSO party id");

        try {
            return scanProxyApi.getDsoPartyId()
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            logger.error("Error fetching DSO party id: {}", ex.getMessage(), ex);
                            currentSpan.recordException(ex);
                        } else {
                            logger.info("Successfully fetched DSO party id: {}", result.getDsoPartyId());
                        }
                    });
        } catch (ApiException e) {
            // cannot happen - OpenAPI codegen adds false checked `throws` declaration
            logger.error("Unexpected ApiException thrown while fetching DSO party id", e);
            throw new RuntimeException(e);
        }
    }

    @WithSpan
    public CompletableFuture<GetAmuletRulesProxyResponse> getAmuletRules() {
        Span currentSpan = Span.current();
        currentSpan.setAttribute("backend.apiName", "ScanProxy");
        currentSpan.setAttribute("backend.method", "getAmuletRules");

        logger.debug("Fetching AmuletRules");

        try {
            return scanProxyApi.getAmuletRules()
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            logger.error("Error fetching AmuletRules: {}", ex.getMessage(), ex);
                            currentSpan.recordException(ex);
                        } else {
                            logger.info("Successfully fetched AmuletRules: {}", result);
                        }
                    });
        } catch (ApiException e) {
            // cannot happen - OpenAPI codegen adds false checked `throws` declaration
            logger.error("Unexpected ApiException thrown while fetching AmuletRules", e);
            throw new RuntimeException(e);
        }
    }

    @WithSpan
    public CompletableFuture<GetOpenAndIssuingMiningRoundsProxyResponse> getOpenAndIssuingMiningRounds() {
        Span currentSpan = Span.current();
        currentSpan.setAttribute("backend.apiName", "ScanProxy");
        currentSpan.setAttribute("backend.method", "getOpenAndIssuingMiningRounds");

        logger.debug("Fetching Open and Issuing MiningRounds");

        try {
            return scanProxyApi.getOpenAndIssuingMiningRounds()
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            logger.error("Error fetching Open and Issuing MiningRounds: {}", ex.getMessage(), ex);
                            currentSpan.recordException(ex);
                        } else {
                            logger.info("Successfully fetched Open and Issuing MiningRounds: {}", result);
                        }
                    });
        } catch (ApiException e) {
            // cannot happen - OpenAPI codegen adds false checked `throws` declaration
            logger.error("Unexpected ApiException thrown while fetching Open and Issuing MiningRounds", e);
            throw new RuntimeException(e);
        }
    }
}
