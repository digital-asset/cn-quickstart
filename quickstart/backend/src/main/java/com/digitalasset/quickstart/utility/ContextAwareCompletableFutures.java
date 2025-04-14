// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.utility;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class ContextAwareCompletableFutures {

    private ContextAwareCompletableFutures() {
        // Utility class: prevent instantiation
    }

    /**
     * Returns a Supplier<T> that makes the provided parentContext current
     * before invoking the original supplier.
     */
    public static <T> Supplier<T> supplyWithin(Context parentContext, Supplier<T> supplier) {
        return () -> {
            try (Scope ignored = parentContext.makeCurrent()) {
                return supplier.get();
            }
        };
    }

    /**
     * Returns a BiConsumer<T, U> that makes the provided parentContext current
     * before invoking the original consumer (used in whenComplete or handle).
     */
    public static <T, U> BiConsumer<T, U> completeWithin(Context parentContext, BiConsumer<T, U> consumer) {
        return (t, u) -> {
            try (Scope ignored = parentContext.makeCurrent()) {
                consumer.accept(t, u);
            }
        };
    }
}
