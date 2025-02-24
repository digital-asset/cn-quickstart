// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.utility;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import org.slf4j.Logger;

import java.util.Map;

public final class LoggingSpanHelper {

    private LoggingSpanHelper() {
        // Utility class: prevent instantiation
    }

    /**
     * Add attributes to the current Span from a map.
     * If attributes is null, does nothing.
     */
    public static void setSpanAttributes(Span span, Map<String, Object> attributes) {
        if (span == null || attributes == null) {
            return;
        }
        attributes.forEach((key, value) -> {
            if (value != null) {
                span.setAttribute(key, value.toString());
            } else {
                span.setAttribute(key, "");
            }
        });
    }

    /**
     * Add an event to the span with optional attributes.
     * If attributes is null, just add the event name with no extra attributes.
     */
    public static void addEventWithAttributes(Span span, String eventName, Map<String, Object> attributes) {
        if (span == null) {
            return;
        }
        if (attributes == null) {
            span.addEvent(eventName);
            return;
        }

        AttributesBuilder attrBuilder = Attributes.builder();
        attributes.forEach((k, v) -> {
            if (v != null) {
                attrBuilder.put(k, v.toString());
            }
        });
        span.addEvent(eventName, attrBuilder.build());
    }

    /**
     * Record an exception in the span and set the Span status to ERROR.
     */
    public static void recordException(Span span, Throwable t) {
        if (span != null && t != null) {
            span.recordException(t);
            span.setStatus(StatusCode.ERROR, t.getMessage());
        }
    }

    // INFO

    public static void logInfo(Logger logger, String message, Map<String, Object> attributes) {
        if (logger == null) {
            return;
        }
        if (attributes == null) {
            // no attributes
            logger.atInfo().log(message);
        } else {
            var logBuilder = logger.atInfo();
            attributes.forEach(logBuilder::addKeyValue);
            logBuilder.log(message);
        }
    }

    public static void logInfo(Logger logger, String message) {
        if (logger == null) {
            return;
        }
        logger.atInfo().log(message);
    }

    // DEBUG

    public static void logDebug(Logger logger, String message, Map<String, Object> attributes) {
        if (logger == null) {
            return;
        }
        if (attributes == null) {
            logger.atDebug().log(message);
        } else {
            var logBuilder = logger.atDebug();
            attributes.forEach(logBuilder::addKeyValue);
            logBuilder.log(message);
        }
    }

    public static void logDebug(Logger logger, String message) {
        if (logger == null) {
            return;
        }
        logger.atDebug().log(message);
    }

    // ERROR

    public static void logError(Logger logger, String message, Map<String, Object> attributes, Throwable t) {
        if (logger == null) {
            return;
        }
        var logBuilder = logger.atError();
        if (attributes != null) {
            attributes.forEach(logBuilder::addKeyValue);
        }
        if (t != null) {
            logBuilder.setCause(t);
        }
        logBuilder.log(message);
    }

    public static void logError(Logger logger, String message, Throwable t) {
        if (logger == null) {
            return;
        }
        var logBuilder = logger.atError();
        if (t != null) {
            logBuilder.setCause(t);
        }
        logBuilder.log(message);
    }

    public static void logError(Logger logger, String message) {
        if (logger == null) {
            return;
        }
        logger.atError().log(message);
    }
}
