// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.utility;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import org.slf4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

/**
 * The {@code LoggingSpanHelper} is a utility class designed to centralize common logging
 * and OpenTelemetry trace operations. Its primary purpose is to reduce repetitive code
 * when adding the same structured attributes to both log statements and the current
 * OpenTelemetry {@link Span}.
 *
 * <p>This helps ensure consistency between logs and tracing data, making it easier to
 * correlate log entries with specific span attributes and events. By standardizing
 * attribute handling, you also avoid the risk of typos or mismatched field names in
 * separate logging vs. tracing code paths.
 *
 * <p>Typical usage includes:
 * <ul>
 *   <li>Setting top-level attributes on a span via {@link #setSpanAttributes(Span, Map)}.
 *   <li>Adding events (with attributes) to a span via {@link #addEventWithAttributes(Span, String, Map)}.
 *   <li>Logging at various levels (INFO, DEBUG, ERROR) with the same structured attributes.
 *   <li>Recording exceptions on both logs and spans consistently.
 * </ul>
 *
 * <p>The methods in this class should be used when you need to:
 * <ol>
 *   <li>Log key contextual data (e.g., commandId, contractId) as structured fields.</li>
 *   <li>Set or record the same data on the current span for distributed tracing.</li>
 *   <li>Maintain consistency across your logs and telemetry.</li>
 * </ol>
 * <p>
 * If your code does not require adding attributes to spans or correlating them with logs,
 * you can bypass this helper and use standard logging calls directly.
 */
public final class LoggingSpanHelper {

    private LoggingSpanHelper() {
        // Utility class: prevent instantiation
    }

    /**
     * Add attributes to the current Span from a map.
     * If attributes is null, does nothing.
     *
     * @param span       the current OpenTelemetry Span; may be null
     * @param attributes the map of key-value attributes; may be null
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
     *
     * @param span       the current OpenTelemetry Span; may be null
     * @param eventName  the name of the event
     * @param attributes the map of key-value attributes to attach to the event; may be null
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
     *
     * @param span the current OpenTelemetry Span; may be null
     * @param t    the Throwable to record; may be null
     */
    public static void recordException(Span span, Throwable t) {
        if (span != null && t != null) {
            span.recordException(t);
            span.setStatus(StatusCode.ERROR, t.getMessage());
        }
    }

    // INFO

    /**
     * Log an INFO-level message with optional structured attributes.
     *
     * @param logger     the SLF4J logger; may be null
     * @param message    the log message
     * @param attributes the map of key-value attributes; may be null
     */
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

    /**
     * Log an INFO-level message without attributes.
     *
     * @param logger  the SLF4J logger; may be null
     * @param message the log message
     */
    public static void logInfo(Logger logger, String message) {
        if (logger == null) {
            return;
        }
        logger.atInfo().log(message);
    }

    // DEBUG

    /**
     * Log a DEBUG-level message with optional structured attributes.
     *
     * @param logger     the SLF4J logger; may be null
     * @param message    the log message
     * @param attributes the map of key-value attributes; may be null
     */
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

    /**
     * Log a DEBUG-level message without attributes.
     *
     * @param logger  the SLF4J logger; may be null
     * @param message the log message
     */
    public static void logDebug(Logger logger, String message) {
        if (logger == null) {
            return;
        }
        logger.atDebug().log(message);
    }

    // ERROR

    /**
     * Log an ERROR-level message with optional structured attributes and a throwable.
     *
     * @param logger     the SLF4J logger; may be null
     * @param message    the log message
     * @param attributes the map of key-value attributes; may be null
     * @param t          the throwable to include in the log; may be null
     */
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
        logger.error(message + " WHY THIS WAS NOT LOGGED: " + t.getMessage(), t);
    }

    /**
     * Log an ERROR-level message and a throwable, without additional attributes.
     *
     * @param logger  the SLF4J logger; may be null
     * @param message the log message
     * @param t       the throwable to include in the log; may be null
     */
    public static void logError(Logger logger, String message, Throwable t) {
        if (logger == null) {
            return;
        }

        var logBuilder = logger.atError();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        if (t != null) {
            logBuilder.setCause(t);
            t.printStackTrace(pw);
        }
        logBuilder.log(message + " WHY THIS WAS NOT LOGGED: " +t.getMessage() + "\n" + sw);
    }

    /**
     * Log an ERROR-level message without attributes or throwable details.
     *
     * @param logger  the SLF4J logger; may be null
     * @param message the log message
     */
    public static void logError(Logger logger, String message) {
        if (logger == null) {
            return;
        }
        logger.atError().log(message);
    }
}
