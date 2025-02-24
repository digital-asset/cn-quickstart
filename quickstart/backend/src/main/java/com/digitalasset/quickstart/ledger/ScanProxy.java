// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.ledger;

import com.digitalasset.quickstart.utility.LoggingSpanHelper;
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

import java.util.Map;
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
        Span span = Span.current();
        LoggingSpanHelper.logDebug(logger, "Fetching DSO party id");
        try {
            return scanProxyApi.getDsoPartyId()
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            LoggingSpanHelper.logError(logger, "Error fetching DSO party id", ex);
                            LoggingSpanHelper.recordException(span, ex);
                        } else {
                            Map<String, Object> attributes = Map.of("dsoPartyId", result.getDsoPartyId());
                            LoggingSpanHelper.setSpanAttributes(span, attributes);
                            LoggingSpanHelper.logInfo(logger, "Successfully fetched DSO party id", attributes);
                        }
                    });
        } catch (ApiException e) {
            // should not be possible - OpenAPI codegen adds false checked `throws` declaration
            LoggingSpanHelper.logError(logger, "Unexpected ApiException thrown while fetching DSO party id", e);
            LoggingSpanHelper.recordException(span, e);
            throw new RuntimeException(e);
        }
    }

    @WithSpan
    public CompletableFuture<GetAmuletRulesProxyResponse> getAmuletRules() {
        Span span = Span.current();
        LoggingSpanHelper.logDebug(logger, "Fetching AmuletRules");
        try {
            return scanProxyApi.getAmuletRules()
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            LoggingSpanHelper.logError(logger, "Error fetching AmuletRules", ex);
                            LoggingSpanHelper.recordException(span, ex);
                        } else {
                            Map<String, Object> attributes = Map.of("amuletRules", result);
                            LoggingSpanHelper.setSpanAttributes(span, attributes);
                            LoggingSpanHelper.logInfo(logger, "Successfully fetched AmuletRules", attributes);
                        }
                    });
        } catch (ApiException e) {
            // should not be possible - OpenAPI codegen adds false checked `throws` declaration
            LoggingSpanHelper.logError(logger, "Unexpected ApiException thrown while fetching AmuletRules", e);
            LoggingSpanHelper.recordException(span, e);
            throw new RuntimeException(e);
        }
    }

    @WithSpan
    public CompletableFuture<GetOpenAndIssuingMiningRoundsProxyResponse> getOpenAndIssuingMiningRounds() {
        Span span = Span.current();
        LoggingSpanHelper.logDebug(logger, "Fetching Open and Issuing MiningRounds");
        try {
            return scanProxyApi.getOpenAndIssuingMiningRounds()
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            LoggingSpanHelper.logError(logger, "Error fetching Open and Issuing MiningRounds", ex);
                            LoggingSpanHelper.recordException(span, ex);
                        } else {
                            Map<String, Object> attributes = Map.of("openAndIssuingMiningRounds", result);
                            LoggingSpanHelper.setSpanAttributes(span, attributes);
                            LoggingSpanHelper.logInfo(logger, "Successfully fetched Open and Issuing MiningRounds", attributes);
                        }
                    });
        } catch (ApiException e) {
            // should not be possible - OpenAPI codegen adds false checked `throws` declaration
            LoggingSpanHelper.logError(logger, "Unexpected ApiException thrown while fetching Open and Issuing MiningRounds", e);
            LoggingSpanHelper.recordException(span, e);
            throw new RuntimeException(e);
        }
    }
}
