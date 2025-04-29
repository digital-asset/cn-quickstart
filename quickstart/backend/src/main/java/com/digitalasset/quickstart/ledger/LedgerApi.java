// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.ledger;

import com.daml.ledger.api.v2.*;
import com.digitalasset.quickstart.config.LedgerConfig;
import com.digitalasset.quickstart.security.TokenProvider;
import com.digitalasset.quickstart.utility.LoggingSpanHelper;
import com.digitalasset.transcode.Converter;
import com.digitalasset.transcode.codec.proto.ProtobufCodec;
import com.digitalasset.transcode.java.Choice;
import com.digitalasset.transcode.java.ContractId;
import com.digitalasset.transcode.java.Template;
import com.digitalasset.transcode.java.Utils;
import com.digitalasset.transcode.schema.Dictionary;
import com.digitalasset.transcode.schema.Identifier;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import daml.Daml;
import io.grpc.*;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class LedgerApi {
    private final String APP_ID ;
    private final CommandSubmissionServiceGrpc.CommandSubmissionServiceFutureStub submission;
    private final CommandServiceGrpc.CommandServiceFutureStub commands;
    private final Dictionary<Converter<Object, ValueOuterClass.Value>> dto2Proto;
    private final Dictionary<Converter<ValueOuterClass.Value, Object>> proto2Dto;

    private final Logger logger = LoggerFactory.getLogger(LedgerApi.class);

    @Autowired
    public LedgerApi(LedgerConfig ledgerConfig, TokenProvider tokenProvider) {
        APP_ID = ledgerConfig.getApplicationId();
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(ledgerConfig.getHost(), ledgerConfig.getPort())
                .usePlaintext()
                .intercept(new Interceptor(tokenProvider))
                .build();

        // Single log statement, not duplicating attributes for spans, so leaving as-is:
        logger.atInfo()
                .addKeyValue("host", ledgerConfig.getHost())
                .addKeyValue("port", ledgerConfig.getPort())
                .log("Connected to ledger");

        submission = CommandSubmissionServiceGrpc.newFutureStub(channel);
        commands = CommandServiceGrpc.newFutureStub(channel);

        ProtobufCodec protoCodec = new ProtobufCodec();
        dto2Proto = Utils.getConverters(Daml.ENTITIES, protoCodec);
        proto2Dto = Utils.getConverters(protoCodec, Daml.ENTITIES);
    }

    @WithSpan
    public <T extends Template> CompletableFuture<Void> create(
            @SpanAttribute("backend.party") String party,
            T entity,
            String commandId
    ) {
        Span currentSpan = Span.current();

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("commandId", commandId);
        attrs.put("templateId", entity.templateId().toString());
        attrs.put("applicationId", APP_ID);
        attrs.put("party", party);

        LoggingSpanHelper.setSpanAttributes(currentSpan, attrs);
        LoggingSpanHelper.logDebug(logger, "Creating contract", attrs);

        CommandsOuterClass.Command.Builder command = CommandsOuterClass.Command.newBuilder();
        ValueOuterClass.Value payload = dto2Proto.template(entity.templateId()).convert(entity);

        command.getCreateBuilder()
                .setTemplateId(toIdentifier(entity.templateId()))
                .setCreateArguments(payload.getRecord());

        LoggingSpanHelper.addEventWithAttributes(currentSpan, "built ledger create command", attrs);

        return submitCommands(party, List.of(command.build()), commandId)
                .<Void>thenApply(submitResponse -> null)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        LoggingSpanHelper.logError(logger, "Failed to create contract", attrs, ex);
                        LoggingSpanHelper.recordException(currentSpan, ex);
                    } else {
                        LoggingSpanHelper.logInfo(logger, "Successfully submitted create command", attrs);
                    }
                });
    }

    @WithSpan
    public <T extends Template, Result, C extends Choice<T, Result>>
    CompletableFuture<Result> exerciseAndGetResult(
            @SpanAttribute("backend.party") String party,
            ContractId<T> contractId,
            C choice,
            String commandId
    ) {
        return exerciseAndGetResult(party, contractId, choice, commandId, List.of());
    }

    @WithSpan
    public <T extends Template, Result, C extends Choice<T, Result>>
    CompletableFuture<Result> exerciseAndGetResult(
            @SpanAttribute("backend.party") String party,
            ContractId<T> contractId,
            C choice,
            String commandId,
            List<CommandsOuterClass.DisclosedContract> disclosedContracts
    ) {
        Span currentSpan = Span.current();

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("commandId", commandId);
        attrs.put("contractId", contractId.getContractId);
        attrs.put("choiceName", choice.choiceName());
        attrs.put("templateId", choice.templateId().toString());
        attrs.put("applicationId", APP_ID);
        attrs.put("party", party);

        LoggingSpanHelper.setSpanAttributes(currentSpan, attrs);
        LoggingSpanHelper.logDebug(logger, "Exercising choice", attrs);

        CommandsOuterClass.Command.Builder cmdBuilder = CommandsOuterClass.Command.newBuilder();
        ValueOuterClass.Value payload =
                dto2Proto.choiceArgument(choice.templateId(), choice.choiceName()).convert(choice);

        cmdBuilder.getExerciseBuilder()
                .setTemplateId(toIdentifier(choice.templateId()))
                .setContractId(contractId.getContractId)
                .setChoice(choice.choiceName())
                .setChoiceArgument(payload);

        CommandsOuterClass.Commands.Builder commandsBuilder = CommandsOuterClass.Commands.newBuilder()
                .setCommandId(commandId)
                .addActAs(party)
                .addReadAs(party)
                .addCommands(cmdBuilder.build());

        if (disclosedContracts != null && !disclosedContracts.isEmpty()) {
            commandsBuilder.addAllDisclosedContracts(disclosedContracts);
        }

        CommandServiceOuterClass.SubmitAndWaitRequest request =
                CommandServiceOuterClass.SubmitAndWaitRequest.newBuilder()
                        .setCommands(commandsBuilder.build())
                        .build();

        LoggingSpanHelper.addEventWithAttributes(currentSpan, "built ledger submit request", attrs);
        LoggingSpanHelper.logInfo(logger, "Submitting ledger command", attrs);

        return toCompletableFuture(commands.submitAndWaitForTransactionTree(request))
                .thenApply(response -> {
                    TransactionOuterClass.TransactionTree txTree = response.getTransaction();
                    long offset = txTree.getOffset();
                    String workflowId = txTree.getWorkflowId();
                    String rootEventId = "";
                    TransactionOuterClass.TreeEvent event = txTree.getEventsByIdMap().get(rootEventId);
                    String eventId = event != null ? rootEventId : null;

                    Map<String, Object> completionAttrs = new HashMap<>(attrs);
                    completionAttrs.put("ledgerOffset", offset);
                    completionAttrs.put("workflowId", workflowId);
                    if (eventId != null) {
                        completionAttrs.put("eventId", eventId);
                    }

                    LoggingSpanHelper.setSpanAttributes(currentSpan, completionAttrs);
                    LoggingSpanHelper.logInfo(logger, "Exercised choice", completionAttrs);

                    ValueOuterClass.Value resultPayload =
                            event != null ? event.getExercised().getExerciseResult() : ValueOuterClass.Value.getDefaultInstance();

                    @SuppressWarnings("unchecked")
                    Result result = (Result) proto2Dto.choiceResult(choice.templateId(), choice.choiceName())
                            .convert(resultPayload);
                    return result;
                })
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        LoggingSpanHelper.logError(logger, "Failed to exercise choice", attrs, ex);
                        LoggingSpanHelper.recordException(currentSpan, ex);
                    } else {
                        LoggingSpanHelper.logInfo(logger, "Completed exercising choice", attrs);
                    }
                });
    }

    @WithSpan
    public CompletableFuture<CommandSubmissionServiceOuterClass.SubmitResponse> submitCommands(
            @SpanAttribute("backend.party") String party,
            List<CommandsOuterClass.Command> cmds,
            String commandId
    ) {
        return submitCommands(party, cmds, commandId, List.of());
    }

    @WithSpan
    public CompletableFuture<CommandSubmissionServiceOuterClass.SubmitResponse> submitCommands(
            @SpanAttribute("backend.party") String party,
            List<CommandsOuterClass.Command> cmds,
            String commandId,
            List<CommandsOuterClass.DisclosedContract> disclosedContracts
    ) {
        Span currentSpan = Span.current();

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("party", party);
        attrs.put("commands.count", cmds.size());
        attrs.put("commandId", commandId);
        attrs.put("applicationId", APP_ID);

        LoggingSpanHelper.setSpanAttributes(currentSpan, attrs);
        LoggingSpanHelper.logInfo(logger, "Submitting commands", attrs);

        CommandsOuterClass.Commands.Builder commandsBuilder = CommandsOuterClass.Commands.newBuilder()
                .setCommandId(commandId)
                .addActAs(party)
                .addReadAs(party)
                .addAllCommands(cmds);

        if (disclosedContracts != null && !disclosedContracts.isEmpty()) {
            commandsBuilder.addAllDisclosedContracts(disclosedContracts);
        }

        CommandSubmissionServiceOuterClass.SubmitRequest request =
                CommandSubmissionServiceOuterClass.SubmitRequest.newBuilder()
                        .setCommands(commandsBuilder.build())
                        .build();

        return toCompletableFuture(submission.submit(request))
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        LoggingSpanHelper.logError(logger, "Failed to submit commands", attrs, ex);
                        LoggingSpanHelper.recordException(currentSpan, ex);
                    } else {
                        LoggingSpanHelper.logInfo(logger, "Successfully submitted commands", attrs);
                    }
                });
    }

    private static <T> CompletableFuture<T> toCompletableFuture(ListenableFuture<T> listenableFuture) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        Futures.addCallback(listenableFuture, new FutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
                completableFuture.complete(result);
            }

            @Override
            public void onFailure(Throwable t) {
                completableFuture.completeExceptionally(t);
            }
        }, MoreExecutors.directExecutor());
        return completableFuture;
    }

    private static ValueOuterClass.Identifier toIdentifier(Identifier id) {
        return ValueOuterClass.Identifier.newBuilder()
                .setPackageId(id.packageNameAsPackageId())
                .setModuleName(id.moduleName())
                .setEntityName(id.entityName())
                .build();
    }


    private static class Interceptor implements ClientInterceptor {
        private final Metadata.Key<String> AUTHORIZATION_HEADER = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
        private final TokenProvider tokenProvider;

        public Interceptor(TokenProvider tokenProvider) {
            this.tokenProvider = tokenProvider;
        }

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
            ClientCall<ReqT, RespT> clientCall = next.newCall(method, callOptions);
            return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(clientCall) {
                @Override
                public void start(Listener<RespT> responseListener, Metadata headers) {
                    headers.put(AUTHORIZATION_HEADER, "Bearer " + tokenProvider.getToken());
                    super.start(responseListener, headers);
                }
            };
        }
    }
}
