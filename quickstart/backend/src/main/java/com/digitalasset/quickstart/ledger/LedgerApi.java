// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.ledger;

import com.daml.ledger.api.v2.*;
import com.digitalasset.quickstart.config.LedgerConfig;
import com.digitalasset.quickstart.security.AuthUtils;
import com.digitalasset.quickstart.security.TokenProvider;
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
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.digitalasset.quickstart.utility.LoggingSpanHelper.*;

@Component
public class LedgerApi {
    private final String APP_ID;
    private final CommandSubmissionServiceGrpc.CommandSubmissionServiceFutureStub submission;
    private final CommandServiceGrpc.CommandServiceFutureStub commands;
    private final Dictionary<Converter<Object, ValueOuterClass.Value>> dto2Proto;
    private final Dictionary<Converter<ValueOuterClass.Value, Object>> proto2Dto;

    private final Logger logger = LoggerFactory.getLogger(LedgerApi.class);
    private final String appProviderParty;

    @Autowired
    public LedgerApi(LedgerConfig ledgerConfig, Optional<TokenProvider> tokenProvider, AuthUtils authUtils) {
        APP_ID = ledgerConfig.getApplicationId();
        appProviderParty = authUtils.getAppProviderPartyId();
        ManagedChannelBuilder<?> builder = ManagedChannelBuilder
                .forAddress(ledgerConfig.getHost(), ledgerConfig.getPort())
                .usePlaintext();
        if (tokenProvider.isEmpty()) {
            throw new IllegalStateException("TokenProvider is required for authentication");
        }
        builder.intercept(new Interceptor(tokenProvider.get()));
        ManagedChannel channel = builder.build();

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
            T entity,
            String commandId
    ) {
        return traceWithStartEvent(
                logger,
                "Submitting create command",
                Map.of(
                        "commandId", commandId,
                        "templateId", entity.templateId().toString(),
                        "applicationId", APP_ID
                ),
                () -> {
                    CommandsOuterClass.Command.Builder command = CommandsOuterClass.Command.newBuilder();
                    ValueOuterClass.Value payload = dto2Proto.template(entity.templateId()).convert(entity);
                    command.getCreateBuilder().setTemplateId(toIdentifier(entity.templateId())).setCreateArguments(payload.getRecord());
                    return submitCommands(List.of(command.build()), commandId).<Void>thenApply(submitResponse -> null);
                }
        );
    }

    @WithSpan
    public <T extends Template, Result, C extends Choice<T, Result>>
    CompletableFuture<Result> exerciseAndGetResult(
            ContractId<T> contractId,
            C choice,
            String commandId
    ) {
        return exerciseAndGetResult(contractId, choice, commandId, List.of());
    }

    @WithSpan
    public <T extends Template, Result, C extends Choice<T, Result>>
    CompletableFuture<Result> exerciseAndGetResult(
            ContractId<T> contractId,
            C choice,
            String commandId,
            List<CommandsOuterClass.DisclosedContract> disclosedContracts
    ) {
        return trace(
                logger,
                "Exercising choice",
                Map.of(
                        "commandId", commandId,
                        "contractId", contractId.getContractId,
                        "choiceName", choice.choiceName(),
                        "templateId", choice.templateId().toString(),
                        "applicationId", APP_ID
                ),
                () -> {
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
                            .addActAs(appProviderParty)
                            .addReadAs(appProviderParty)
                            .addCommands(cmdBuilder.build());

                    if (disclosedContracts != null && !disclosedContracts.isEmpty()) {
                        commandsBuilder.addAllDisclosedContracts(disclosedContracts);
                    }

                    CommandServiceOuterClass.SubmitAndWaitRequest request =
                            CommandServiceOuterClass.SubmitAndWaitRequest.newBuilder()
                                    .setCommands(commandsBuilder.build())
                                    .build();

                    addEventWithAttributes(Span.current(), "built ledger submit request", Map.of());
                    logger.info("Submitting ledger command");
                    return toCompletableFuture(commands.submitAndWaitForTransactionTree(request))
                            .thenApply(response -> {
                                TransactionOuterClass.TransactionTree txTree = response.getTransaction();
                                long offset = txTree.getOffset();
                                String workflowId = txTree.getWorkflowId();
                                Map<Integer, TransactionOuterClass.TreeEvent> eventsById = txTree.getEventsByIdMap();
                                Integer eventId = eventsById.isEmpty() ? null : Collections.min(eventsById.keySet());
                                TransactionOuterClass.TreeEvent event = eventId != null ? txTree.getEventsByIdMap().get(eventId) : null;

                                Map<String, Object> completionAttrs = new HashMap<>();
                                completionAttrs.put("ledgerOffset", offset);
                                completionAttrs.put("workflowId", workflowId);
                                if (eventId != null) {
                                    completionAttrs.put("eventId", eventId);
                                }

                                setSpanAttributes(Span.current(), completionAttrs);
                                logInfo(logger, "Exercised choice", completionAttrs);

                                ValueOuterClass.Value resultPayload = event != null ? event.getExercised().getExerciseResult() : ValueOuterClass.Value.getDefaultInstance();

                                @SuppressWarnings("unchecked")
                                Result result = (Result) proto2Dto.choiceResult(choice.templateId(), choice.choiceName()).convert(resultPayload);
                                return result;
                            });
                }
        );
    }

    @WithSpan
    public CompletableFuture<CommandSubmissionServiceOuterClass.SubmitResponse> submitCommands(
            List<CommandsOuterClass.Command> cmds,
            String commandId
    ) {
        return submitCommands(cmds, commandId, List.of());
    }

    @WithSpan
    public CompletableFuture<CommandSubmissionServiceOuterClass.SubmitResponse> submitCommands(
            List<CommandsOuterClass.Command> cmds,
            String commandId,
            List<CommandsOuterClass.DisclosedContract> disclosedContracts
    ) {
        return trace(
                logger,
                "Submitting commands",
                Map.of(
                        "commands.count", cmds.size(),
                        "commandId", commandId,
                        "applicationId", APP_ID
                ),
                () -> {
                    CommandsOuterClass.Commands.Builder commandsBuilder = CommandsOuterClass.Commands.newBuilder()
                            .setCommandId(commandId)
                            .addActAs(appProviderParty)
                            .addReadAs(appProviderParty)
                            .addAllCommands(cmds);

                    if (disclosedContracts != null && !disclosedContracts.isEmpty()) {
                        commandsBuilder.addAllDisclosedContracts(disclosedContracts);
                    }

                    CommandSubmissionServiceOuterClass.SubmitRequest request =
                            CommandSubmissionServiceOuterClass.SubmitRequest.newBuilder()
                                    .setCommands(commandsBuilder.build())
                                    .build();

                    return toCompletableFuture(submission.submit(request));
                }
        );
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
