# Logging Style Guide

## 1. Objectives

1. **Structured Logs**: Provide key attributes (e.g. ``commandId``, ``contractId``) as structured fields, rather than embedding them within the message string. This ensures that downstream log aggregation systems (e.g. Loki) can parse and query them easily.

2. **Span Attributes**: Important contextual fields should also be mirrored in OpenTelemetry span attributes so that traces can be filtered and correlated with logs.

3. **Log Levels**:
   - **TRACE**: Rare in production; extremely detailed debug information.
   - **DEBUG**: Developer-level debugging, usually disabled in production.
   - **INFO**: High-level, user-facing/request-lifecycle events, typically enabled in production.
   - **WARN**: Non-critical issues that might need attention.
   - **ERROR**: Failures or unexpected exceptions.

4. **Security/PII**: Never log secrets, passwords, or personally identifiable data. If needed, anonymize or hash such data.

---

## 2. Important Fields & Priorities

When these fields are available, log them as structured key-value pairs.

| Field Name        | Priority | Description                                                                   |
|-------------------|----------|-------------------------------------------------------------------------------|
| **commandId**     | V.High   | A top-level correlation ID for commands. If present, always log.              |
| **choiceName**    | High     | The name of the Daml choice being exercised (where applicable).               |
| **templateId**    | High     | The template identifier (where applicable).                                   |
| **workflowId**    | High     | The unique workflow ID (if any) from the transaction.                         |
| **transactionId** | High     | Daml transaction ID (if known/available).                                     |
| **contractId**    | High     | The contract ID being operated on.                                            |
| **submissionId**  | Mod      | Command submission ID.                                                        |
| **applicationId** | Mod      | Application ID for the Ledger client.                                         |
| **ledgerOffset**  | Low      | Ledger offset in the transaction stream (if known).                           |
| **eventId**       | Low      | Event ID for individual events within a transaction.                          |

---

## 3. Structured Logging Conventions

1. **Log Format**:
   - **Message**: A short, human-readable message describing the event.
   - **Key-Value Fields**: Supply important contextual data using ``addKeyValue()`` rather than embedding data in the message.

   **Example**:
   ```java
   logger.atInfo()
         .addKeyValue("commandId", commandId)
         .addKeyValue("clientId", request.getClientId())
         .addKeyValue("templateId", quickstart_licensing.licensing.appinstall.AppInstallRequest.TEMPLATE_ID.qualifiedName())
         .addKeyValue("party", request.getParty())
         .log("createTenantRegistration - Starting async creation");
   ```

   If the same attributes should be reused later:

   ```java
   Map<String, Object> mainAttrs = Map.of(
       "commandId", commandId,
       "clientId", request.getClientId(),
       "templateId", quickstart_licensing.licensing.appinstall.AppInstallRequest.TEMPLATE_ID.qualifiedName(),
       "party", request.getParty()
   );
   LoggingSpanHelper.logInfo(logger, "createTenantRegistration - Starting async creation", mainAttrs);
   ```

2. **Naming**:
   - Use lowerCamelCase (dot.separated for field keys), consistently across the codebase.

3. **Exception Logging**:
   - Always include the ``Throwable`` (stack trace) if there is an error:
     ```java
     logger.atError()
           .addKeyValue("commandId", commandId)
           .setCause(ex)
           .log("Operation failed");
     ```
   - Include relevant attributes (``contractId``, ``commandId``, etc.) at ERROR level to allow correlation in logs.

---

## 4. OpenTelemetry Span Usage

1. **@WithSpan**:
   - Each API method is annotated with ``@WithSpan``. You can override the default span name if needed.

2. **Span Attributes**:
   - Mirror key fields into span attributes:
     ```java
     Span span = Span.current();
     span.setAttribute("commandId", commandId);
     span.setAttribute("contractId", contractId);
     span.setAttribute("choiceName", "AppInstallRequest_Reject");
     ```
   - Use the same naming convention as in logs.

3. **Span Events**:
   - Use ``span.addEvent("...")`` for major steps in the request.
   - On exceptions:
     ```java
     span.recordException(ex);
     span.setStatus(StatusCode.ERROR, ex.getMessage());
     ```

4. **@SpanAttribute**:
   - You may use ``@SpanAttribute("appInstall.contractId")`` on parameters to auto-set span attributes.

---

## 5. Example Transformations

### Before
```java
logger.info("acceptAppInstallRequest: received request kv",
    kv("contractId", contractId),
    kv("commandId", commandId)
);
```

### After (Recommended)
```java
logger.atInfo()
      .addKeyValue("contractId", contractId)
      .addKeyValue("commandId", commandId)
      .log("acceptAppInstallRequest: received request");
```

And, for an error scenario:
```java
logger.atError()
      .addKeyValue("contractId", contractId)
      .addKeyValue("commandId", commandId)
      .setCause(ex)
      .log("acceptAppInstallRequest: failed");
```

---

## 6. Using ``LoggingSpanHelper``

The ``LoggingSpanHelper`` (``com.digitalasset.quickstart.utility.LoggingSpanHelper``) utility class helps **reduce code duplication** when you want to (1) log attributes and messages, and (2) set or record those same attributes/events on an OpenTelemetry ``Span``. Instead of manually repeating the same set of attributes for both logs and spans, you can pass a single ``Map<String, Object>`` to ``LoggingSpanHelper``. Below are guidelines on when and how to use it:

### 6.1 When to Use ``LoggingSpanHelper``

- **You have attributes** (e.g., ``commandId``, ``contractId``) you want to log **and** set as span attributes or span events.
  - Example: Common attributes (``contractId``, ``commandId``) used in both logs and the current ``Span``.
  - ``LoggingSpanHelper.setSpanAttributes(...)`` or ``LoggingSpanHelper.addEventWithAttributes(...)`` can be used in tandem with ``LoggingSpanHelper.logInfo(...)``, ``logDebug(...)``, etc.

- **You want to record an exception** in both logs and the current ``Span`` in a consistent manner.
  - Example: ``LoggingSpanHelper.logError(logger, message, attributes, ex)`` combined with ``LoggingSpanHelper.recordException(span, ex)``.

Using ``LoggingSpanHelper`` centralizes the logic so that:
- You avoid repetitive code (e.g. ``span.setAttribute``, ``span.addEvent``, ``logger.atInfo()...``).
- Your logs and tracing data remain in sync.

### 6.2 When **Not** to Use ``LoggingSpanHelper``

- **No span attributes are needed** (or you do not need to add any events to the span).
  - If your code does not require correlated attributes between logs and spans, you can **bypass** ``LoggingSpanHelper`` and use standard logging calls:
    ```java
    logger.atInfo().log("Simple log message with no attributes or correlation");
    ```
  - Similarly, if you only have a single attribute to log and do **not** need it in your span, a simple logger call may suffice.

- **Minimal or purely local** logging.
  - If your logging is trivial (e.g., a quick debug statement without relevant attributes), standard logging (``logger.atDebug()``) is fine.

### 6.3 Choosing Between ``setSpanAttributes(...)`` and ``addEventWithAttributes(...)``

``LoggingSpanHelper`` exposes two main mechanisms for storing data on a ``Span``:

1. **``setSpanAttributes(Span, Map<String,Object>)``**
   - Use for **top-level attributes** that you want to persist throughout the span’s lifetime (e.g., ``commandId``, ``contractId``, ``choiceName``).
   - These attributes will be visible for the entire duration of the span, making them ideal for *request-scoped* or *operation-scoped* data.

2. **``addEventWithAttributes(Span, String eventName, Map<String,Object>)``**
   - Use for **discrete events** (or steps) within an operation. This is especially helpful if you want to pinpoint a specific moment in the trace.
   - Example events: “Fetched contract,” “Invoking ledger API,” “Failed validation step,” etc.
   - Attributes added alongside an event are relevant only to that event; they do not remain attached to the entire span.

#### When to Use Both

It’s common to **set top-level attributes** once at the start of a method (e.g., ``commandId``, ``contractId``), and then **add events** with additional contextual info as your method proceeds through various steps (e.g., “Fetched data from DB,” “Exercising choice,” “Building response object”).

A typical pattern might look like this:

```java
Span span = Span.current();

// 1. Set top-level attributes
Map<String, Object> mainAttrs = Map.of(
    "commandId", commandId,
    "contractId", contractId,
    "choiceName", "SomeChoice"
);
LoggingSpanHelper.setSpanAttributes(span, mainAttrs);

// 2. Log these attributes at INFO level (start of the operation)
LoggingSpanHelper.logInfo(logger, "Operation started", mainAttrs);

// 3. Add an event at a specific step
LoggingSpanHelper.addEventWithAttributes(span, "Fetched data from repository", null);

// 4. Further log statements or events as needed...
```

### 6.4 Example Usage

Below is a condensed example from a method that uses ``LoggingSpanHelper`` to handle logging and span updates together:

```java
@WithSpan
public CompletableFuture<ResponseEntity<Void>> cancelAppInstallRequest(
    @SpanAttribute("appInstall.contractId") String contractId,
    @SpanAttribute("appInstall.commandId") String commandId
) {
    Span span = Span.current();

    // Prepare common attributes
    Map<String, Object> attributes = Map.of(
        "contractId", contractId,
        "commandId", commandId,
        "templateId", "quickstart_licensing.licensing.appinstall.AppInstallRequest",
        "choiceName", "AppInstallRequest_Cancel"
    );

    // Add an event with these attributes
    LoggingSpanHelper.addEventWithAttributes(span, "Starting cancelAppInstallRequest", attributes);

    // Set them as top-level attributes for the entire span
    LoggingSpanHelper.setSpanAttributes(span, attributes);

    // Log a message at INFO with the same attributes
    LoggingSpanHelper.logInfo(logger, "cancelAppInstallRequest: received request", attributes);

    // ... additional business logic ...

    // In a .whenComplete block or catch:
    // LoggingSpanHelper.logError(...) // on failure
    // LoggingSpanHelper.recordException(span, ex);

    return CompletableFuture.completedFuture(ResponseEntity.ok().build());
}
```
