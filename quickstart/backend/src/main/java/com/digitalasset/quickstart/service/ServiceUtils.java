package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.utility.TracingUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

class ServiceUtils {

    static <T> T ensurePresent(Optional<T> opt, String message, Object... args) {
        return opt.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, String.format(message, args)));
    }

    /**
     * Wraps a CompletableFuture with tracing, ensuring that any exceptions are properly propagated.
     * To be used exclusively inside the service API implementations.
     *
     * @param ctx  the tracing context
     * @param body the supplier of the CompletableFuture to be traced
     * @param <T>  the type of the result
     * @return a traced CompletableFuture
     */
    static <T> CompletableFuture<T> traceServiceCallAsync(
            TracingUtils.TracingContext ctx,
            Supplier<CompletableFuture<T>> body) {
        return TracingUtils.traceWithStartEventAsync(ctx, body);
    }
}


