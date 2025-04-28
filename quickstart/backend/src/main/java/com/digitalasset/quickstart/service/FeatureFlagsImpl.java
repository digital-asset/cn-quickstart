package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.security.Auth;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import org.openapitools.model.FeatureFlags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.concurrent.CompletableFuture;

import static com.digitalasset.quickstart.utility.ContextAwareCompletableFutures.completeWithin;
import static com.digitalasset.quickstart.utility.ContextAwareCompletableFutures.supplyWithin;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class FeatureFlagsImpl implements com.digitalasset.quickstart.api.FeatureFlagsApi {

    private static final Logger logger = LoggerFactory.getLogger(FeatureFlagsImpl.class);
    private final Auth auth;

    public FeatureFlagsImpl(Auth auth) {
        this.auth = auth;
    }

    @Override
    public CompletableFuture<ResponseEntity<FeatureFlags>> getFeatureFlags() {
        Span methodSpan = Span.current();
        Context parentContext = Context.current();

        methodSpan.addEvent("Starting getFeatureFlags");
        logger.atInfo().log("getFeatureFlags: Received request, retrieving feature flags asynchronously");

        return CompletableFuture
                .supplyAsync(
                        supplyWithin(parentContext, () -> {
                            methodSpan.addEvent("Building feature flags object");

                            FeatureFlags featureFlags = new FeatureFlags();
                            featureFlags.authMode(auth == Auth.OAUTH2 ? FeatureFlags.AuthModeEnum.OAUTH2 : FeatureFlags.AuthModeEnum.NOAUTH);
                            return ResponseEntity.ok(featureFlags);
                        })
                )
                .whenComplete(
                        completeWithin(parentContext, (response, throwable) -> {
                            if (throwable == null) {
                                logger.atInfo()
                                        .log("getFeatureFlags: Completed successfully");
                            } else {
                                logger.atError()
                                        .setCause(throwable)
                                        .log("getFeatureFlags: Failed with error");
                                methodSpan.recordException(throwable);
                                methodSpan.setStatus(StatusCode.ERROR, throwable.getMessage());
                            }
                        })
                );
    }
}
