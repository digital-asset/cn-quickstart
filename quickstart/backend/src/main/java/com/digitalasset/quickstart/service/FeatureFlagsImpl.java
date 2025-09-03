package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.security.AuthUtils;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.openapitools.model.FeatureFlags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.concurrent.CompletableFuture;

import static com.digitalasset.quickstart.service.ServiceUtils.traceServiceCallAsync;
import static com.digitalasset.quickstart.utility.TracingUtils.tracingCtx;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class FeatureFlagsImpl implements com.digitalasset.quickstart.api.FeatureFlagsApi {

    private static final Logger logger = LoggerFactory.getLogger(FeatureFlagsImpl.class);
    private final AuthUtils auth;

    public FeatureFlagsImpl(AuthUtils auth) {
        this.auth = auth;
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<FeatureFlags>> getFeatureFlags() {
        var ctx = tracingCtx(logger, "getFeatureFlags");
        return traceServiceCallAsync(ctx, () -> CompletableFuture.supplyAsync(() -> {
            FeatureFlags featureFlags = new FeatureFlags();
            featureFlags.authMode(auth.isOAuth2Enabled() ? FeatureFlags.AuthModeEnum.OAUTH2 : FeatureFlags.AuthModeEnum.SHARED_SECRET);
            return ResponseEntity.ok(featureFlags);
        }));
    }
}
