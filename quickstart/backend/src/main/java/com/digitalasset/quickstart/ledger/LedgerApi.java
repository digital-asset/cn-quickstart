// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.ledger;

import com.daml.ledger.api.v2.*;
import com.daml.ledger.api.v2.admin.UserManagementServiceGrpc;
import com.daml.ledger.api.v2.admin.UserManagementServiceOuterClass;
import com.digitalasset.quickstart.config.LedgerConfig;
import com.digitalasset.quickstart.oauth.Interceptor;
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
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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
    private static final String APP_ID;
    private static final String APP_PROVIDER_USER_ID;

    static {
        String appId = System.getenv("AUTH_APP_PROVIDER_CLIENT_ID");
        if (appId == null || appId.isBlank()) {
            throw new IllegalStateException("Environment variable AUTH_APP_PROVIDER_CLIENT_ID is not set");
        }
        APP_ID = appId;
        APP_PROVIDER_USER_ID = appId;
    }

    private final CommandSubmissionServiceGrpc.CommandSubmissionServiceFutureStub submission;
    private final CommandServiceGrpc.CommandServiceFutureStub commands;
    private final UserManagementServiceGrpc.UserManagementServiceFutureStub userManagement;
    private final StateServiceGrpc.StateServiceFutureStub stateService;
    private final Dictionary<Converter<Object, ValueOuterClass.Value>> dto2Proto;
    private final Dictionary<Converter<ValueOuterClass.Value, Object>> proto2Dto;

    private final Logger logger = LoggerFactory.getLogger(LedgerApi.class);

    @Autowired
    public LedgerApi(LedgerConfig ledgerConfig, Interceptor oAuth2ClientInterceptor) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(ledgerConfig.getHost(), ledgerConfig.getPort())
                .usePlaintext()
                .intercept(oAuth2ClientInterceptor)
                .build();

        // Single log statement, not duplicating attributes for spans, so leaving as-is:
        logger.atInfo()
                .addKeyValue("host", ledgerConfig.getHost())
                .addKeyValue("port", ledgerConfig.getPort())
                .log("Connected to ledger");

        submission = CommandSubmissionServiceGrpc.newFutureStub(channel);
        commands = CommandServiceGrpc.newFutureStub(channel);
        userManagement = UserManagementServiceGrpc.newFutureStub(channel);
        stateService = StateServiceGrpc.newFutureStub(channel);

        ProtobufCodec protoCodec = new ProtobufCodec();
        dto2Proto = Utils.getConverters(Daml.ENTITIES, protoCodec);
        proto2Dto = Utils.getConverters(protoCodec, Daml.ENTITIES);
    }

    public CompletableFuture<Void> grantRights(String actAs, String readAs) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("userId", APP_PROVIDER_USER_ID);
        attrs.put("actAs", actAs);
        attrs.put("readAs", readAs);

        LoggingSpanHelper.logDebug(logger, "Attempting to grant user rights", attrs);

        return toCompletableFuture(
                userManagement.grantUserRights(
                        UserManagementServiceOuterClass.GrantUserRightsRequest.newBuilder()
                                .setUserId(APP_PROVIDER_USER_ID)
                                .addAllRights(
                                        List.of(
                                                UserManagementServiceOuterClass.Right.newBuilder()
                                                        .setCanReadAs(
                                                                UserManagementServiceOuterClass.Right.CanReadAs.newBuilder()
                                                                        .setParty(readAs).build()
                                                        ).build(),
                                                UserManagementServiceOuterClass.Right.newBuilder()
                                                        .setCanActAs(
                                                                UserManagementServiceOuterClass.Right.CanActAs.newBuilder()
                                                                        .setParty(actAs).build()
                                                        ).build()
                                        )
                                ).build()
                ))
                .<Void>thenApply(x -> null)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        LoggingSpanHelper.logError(logger, "Failed to grant user rights", attrs, ex);
                    } else {
                        LoggingSpanHelper.logInfo(logger, "Successfully granted user rights", attrs);
                    }
                });
    }

    public CompletableFuture<List<UserManagementServiceOuterClass.Right>> fetchUserRights(String userId) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("userId", userId);

        LoggingSpanHelper.logDebug(logger, "Fetching user rights", attrs);

        CompletableFuture<UserManagementServiceOuterClass.ListUserRightsResponse> response =
                toCompletableFuture(
                        userManagement.listUserRights(
                                UserManagementServiceOuterClass.ListUserRightsRequest.newBuilder().setUserId(userId).build()
                        )
                );

        return response
                .thenApply(UserManagementServiceOuterClass.ListUserRightsResponse::getRightsList)
                .whenComplete((rights, ex) -> {
                    if (ex != null) {
                        LoggingSpanHelper.logError(logger, "Failed to fetch user rights", attrs, ex);
                    } else {
                        Map<String, Object> successAttrs = new HashMap<>(attrs);
                        successAttrs.put("rights.size", rights.size());
                        LoggingSpanHelper.logInfo(logger, "Fetched user rights", successAttrs);
                    }
                });
    }

    public CompletableFuture<UserManagementServiceOuterClass.User> fetchUserInfo(String userId) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("userId", userId);

        LoggingSpanHelper.logDebug(logger, "Fetching user info", attrs);

        UserManagementServiceOuterClass.GetUserRequest request =
                UserManagementServiceOuterClass.GetUserRequest.newBuilder().setUserId(userId).build();

        return toCompletableFuture(userManagement.getUser(request))
                .thenApply(UserManagementServiceOuterClass.GetUserResponse::getUser)
                .whenComplete((user, ex) -> {
                    if (ex != null) {
                        LoggingSpanHelper.logError(logger, "Failed to fetch user info", attrs, ex);
                    } else {
                        Map<String, Object> successAttrs = new HashMap<>(attrs);
                        successAttrs.put("fetchedUserId", user.getId());
                        LoggingSpanHelper.logInfo(logger, "Fetched user info", successAttrs);
                    }
                });
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
                .setApplicationId(APP_ID)
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
                    String eventId =  null;

                    Map<String, Object> completionAttrs = new HashMap<>(attrs);
                    completionAttrs.put("ledgerOffset", offset);
                    completionAttrs.put("workflowId", workflowId);


                    LoggingSpanHelper.setSpanAttributes(currentSpan, completionAttrs);
                    LoggingSpanHelper.logInfo(logger, "Exercised choice", completionAttrs);

                    ValueOuterClass.Value resultPayload =
                            ValueOuterClass.Value.getDefaultInstance();

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
                .setApplicationId(APP_ID)
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
}
