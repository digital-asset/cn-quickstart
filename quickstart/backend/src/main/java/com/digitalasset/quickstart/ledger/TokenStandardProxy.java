// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.ledger;

import com.digitalasset.quickstart.config.LedgerConfig;
import com.digitalasset.quickstart.tokenstandard.openapi.ApiClient;
import com.digitalasset.quickstart.tokenstandard.openapi.ApiException;
import com.digitalasset.quickstart.tokenstandard.openapi.allocation.DefaultAllocationApi;
import com.digitalasset.quickstart.tokenstandard.openapi.allocation.model.ChoiceContext;
import com.digitalasset.quickstart.tokenstandard.openapi.allocation.model.GetChoiceContextRequest;
import com.digitalasset.quickstart.tokenstandard.openapi.metadata.DefaultMetadataApi;
import com.digitalasset.quickstart.tokenstandard.openapi.metadata.model.GetRegistryInfoResponse;
import com.digitalasset.quickstart.utility.LoggingSpanHelper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TokenStandardProxy {
    private final DefaultAllocationApi allocationApi;
    private final DefaultMetadataApi metadataApi;

    private final Logger logger = LoggerFactory.getLogger(TokenStandardProxy.class);

    public TokenStandardProxy(LedgerConfig ledgerConfig) {
        ApiClient apiClient = new ApiClient();
        apiClient.updateBaseUri(ledgerConfig.getRegistryBaseUri());
        this.allocationApi = new DefaultAllocationApi(apiClient);
        this.metadataApi = new DefaultMetadataApi(apiClient);
    }

    @WithSpan
    public CompletableFuture<String> getRegistryAdminId() {
        return trace(() -> metadataApi.getRegistryInfo().thenApply(GetRegistryInfoResponse::getAdminId),
                "Fetching registry admin id");
    }

    @WithSpan
    public CompletableFuture<ChoiceContext> getAllocationTransferContext(String allocationId) {
        return trace(() -> allocationApi.getAllocationTransferContext(allocationId, new GetChoiceContextRequest()),
                "Fetching allocation transfer context", Map.of("allocationId", allocationId));
    }

    // Boilerplate code for logging, tracing and dealing with unrealistic
    // ApiExceptions
    private <T> CompletableFuture<T> trace(ThrowingSupplier<CompletableFuture<T>> supplier, String message,
                                           Map<String, Object> baseAttrs) {
        var span = Span.current();
        LoggingSpanHelper.setSpanAttributes(span, baseAttrs);
        LoggingSpanHelper.logInfo(logger, message, baseAttrs);
        final CompletableFuture<T> cs;
        try {
            cs = supplier.get();
        } catch (ApiException e) {
            // should not be possible - OpenAPI codegen adds false checked `throws`
            // declaration
            LoggingSpanHelper.logError(logger, "Unexpected ApiException thrown while:" + message, baseAttrs, e);
            LoggingSpanHelper.recordException(span, e);
            CompletableFuture<T> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
        return cs.whenComplete((res, ex) -> {
            if (ex != null) {
                LoggingSpanHelper.logError(logger, message + " failed", baseAttrs, ex);
                LoggingSpanHelper.recordException(span, ex);
            } else {
                Map<String, Object> attrsWithResult = new java.util.HashMap<>(baseAttrs);
                attrsWithResult.put("result", res);
                LoggingSpanHelper.logInfo(logger, message + " succeeded", attrsWithResult);
            }
        });
    }

    private <T> CompletableFuture<T> trace(ThrowingSupplier<CompletableFuture<T>> supplier, String message) {
        return trace(supplier, message, Map.of());
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws ApiException;
    }
}
