package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.utility.TracingUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

class ServiceUtils {

    static <T> T ensurePresent(Optional<T> opt, String message, Object... args) {
        return opt.orElseThrow(() -> new ServiceException(HttpStatus.PRECONDITION_FAILED, message, args));
    }

    @SuppressWarnings("unchecked")
    static <T> CompletableFuture<T> traceServiceCallAsync(
            TracingUtils.TracingContext ctx,
            Supplier<CompletableFuture<T>> body) {
        return TracingUtils.traceWithStartEventAsync(ctx, () ->
                body.get().exceptionally(t -> {
                    if (t.getCause() instanceof ServiceException e) {
                        ctx.logger().warn(e.getMessage(), e);
                        return (T) ResponseEntity.status(e.getCode()).body(e.getMessage());
                    }
                    throw new CompletionException(t);
                })
        );
    }
}


