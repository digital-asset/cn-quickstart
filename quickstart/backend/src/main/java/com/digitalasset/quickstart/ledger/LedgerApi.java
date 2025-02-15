// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.ledger;

import com.daml.ledger.api.v2.*;
import com.daml.ledger.api.v2.admin.UserManagementServiceGrpc;
import com.daml.ledger.api.v2.admin.UserManagementServiceOuterClass;
import com.digitalasset.quickstart.config.LedgerConfig;
import com.digitalasset.quickstart.oauth.Interceptor;
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

import java.util.List;
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
        logger.info("Connected to ledger at {}:{}", ledgerConfig.getHost(), ledgerConfig.getPort());
        submission = CommandSubmissionServiceGrpc.newFutureStub(channel);
        commands = CommandServiceGrpc.newFutureStub(channel);
        userManagement = UserManagementServiceGrpc.newFutureStub(channel);
        stateService = StateServiceGrpc.newFutureStub(channel);

        ProtobufCodec protoCodec = new ProtobufCodec();
        dto2Proto = Utils.getConverters(Daml.ENTITIES, protoCodec);
        proto2Dto = Utils.getConverters(protoCodec, Daml.ENTITIES);
    }

    public CompletableFuture<Void> grantRights(String actAs, String readAs) {
        return toCompletableFuture(userManagement.grantUserRights(
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
        )).thenApply(x -> null);
    }

    public CompletableFuture<List<UserManagementServiceOuterClass.Right>> fetchUserRights(String userId) {
        CompletableFuture<UserManagementServiceOuterClass.ListUserRightsResponse> response =
                toCompletableFuture(userManagement.listUserRights(
                        UserManagementServiceOuterClass.ListUserRightsRequest.newBuilder().setUserId(userId).build()
                ));
        return response.thenApply(UserManagementServiceOuterClass.ListUserRightsResponse::getRightsList);
    }

    public CompletableFuture<UserManagementServiceOuterClass.User> fetchUserInfo(String userId) {
        UserManagementServiceOuterClass.GetUserRequest request =
                UserManagementServiceOuterClass.GetUserRequest.newBuilder().setUserId(userId).build();
        return toCompletableFuture(userManagement.getUser(request))
                .thenApply(UserManagementServiceOuterClass.GetUserResponse::getUser);
    }

    @WithSpan
    public <T extends Template> CompletableFuture<Void> create(
            @SpanAttribute("backend.party") String party,
            T entity,
            String commandId
    ) {
        Span currentSpan = Span.current();
        currentSpan.setAttribute("backend.commandId", commandId);
        currentSpan.setAttribute("backend.templateId", entity.templateId().toString());

        CommandsOuterClass.Command.Builder command = CommandsOuterClass.Command.newBuilder();

        // Convert the entity to a ledger create command
        ValueOuterClass.Value payload = dto2Proto.template(entity.templateId()).convert(entity);
        currentSpan.addEvent("converted payload from DTO to Proto");

        command
                .getCreateBuilder()
                .setTemplateId(toIdentifier(entity.templateId()))
                .setCreateArguments(payload.getRecord());
        currentSpan.addEvent("built ledger create command");

        // Now submit the command(s) using the user-provided commandId
        return submitCommands(party, List.of(command.build()), commandId)
                .thenApply(submitResponse -> null);
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
        // Set useful attributes for logging/tracing
        currentSpan.setAttribute("backend.commandId", commandId);
        currentSpan.setAttribute("backend.contractId", contractId.getContractId);
        currentSpan.setAttribute("backend.choiceName", choice.choiceName());

        // Build the single command
        CommandsOuterClass.Command.Builder command = CommandsOuterClass.Command.newBuilder();
        ValueOuterClass.Value payload =
                dto2Proto.choiceArgument(choice.templateId(), choice.choiceName()).convert(choice);

        command.getExerciseBuilder()
                .setTemplateId(toIdentifier(choice.templateId()))
                .setContractId(contractId.getContractId)
                .setChoice(choice.choiceName())
                .setChoiceArgument(payload);
        currentSpan.addEvent("built ledger exercise command");

        // Build the SubmitAndWaitRequest with the given commandId
        CommandsOuterClass.Commands.Builder commandsBuilder = CommandsOuterClass.Commands.newBuilder()
                .setApplicationId(APP_ID)
                .setCommandId(commandId)
                .addActAs(party)
                .addReadAs(party)
                .addCommands(command.build());

        if (disclosedContracts != null && !disclosedContracts.isEmpty()) {
            commandsBuilder.addAllDisclosedContracts(disclosedContracts);
        }

        CommandServiceOuterClass.SubmitAndWaitRequest request =
                CommandServiceOuterClass.SubmitAndWaitRequest.newBuilder()
                        .setCommands(commandsBuilder.build())
                        .build();
        currentSpan.addEvent("built ledger submit request");

        // Submit and decode the exercise result
        return toCompletableFuture(commands.submitAndWaitForTransactionTree(request))
                .thenApply(response -> {
                    currentSpan.addEvent("received ledger submit response");

                    // The "rootEventIds" should have exactly one root event for a single command
                    String eventId = response.getTransaction().getRootEventIds(0);
                    currentSpan.setAttribute("backend.submit.response.event.id", eventId);

                    TransactionOuterClass.TreeEvent event = response.getTransaction().getEventsByIdMap().get(eventId);
                    ValueOuterClass.Value resultPayload = event.getExercised().getExerciseResult();
                    // Convert the result payload back to the type <Result>
                    return (Result) proto2Dto.choiceResult(choice.templateId(), choice.choiceName()).convert(resultPayload);
                });
    }

    @WithSpan
    public CompletableFuture<CommandSubmissionServiceOuterClass.SubmitResponse> submitCommands(
            @SpanAttribute("backend.party") String party,
            List<CommandsOuterClass.Command> commands,
            String commandId
    ) {
        return submitCommands(party, commands, commandId, List.of());
    }

    @WithSpan
    public CompletableFuture<CommandSubmissionServiceOuterClass.SubmitResponse> submitCommands(
            @SpanAttribute("backend.party") String party,
            List<CommandsOuterClass.Command> commands,
            String commandId,
            List<CommandsOuterClass.DisclosedContract> disclosedContracts
    ) {
        Span currentSpan = Span.current();
        currentSpan.setAttribute("backend.commands.count", commands.size());
        currentSpan.setAttribute("backend.commandId", commandId);

        logger.info("Party {} submits {} commands with commandId={}", party, commands.size(), commandId);

        // Use the caller-provided commandId
        CommandsOuterClass.Commands.Builder commandsBuilder = CommandsOuterClass.Commands.newBuilder()
                .setApplicationId(APP_ID)
                .setCommandId(commandId)
                .addActAs(party)
                .addReadAs(party)
                .addAllCommands(commands);

        if (disclosedContracts != null && !disclosedContracts.isEmpty()) {
            commandsBuilder.addAllDisclosedContracts(disclosedContracts);
        }

        CommandSubmissionServiceOuterClass.SubmitRequest request =
                CommandSubmissionServiceOuterClass.SubmitRequest.newBuilder()
                        .setCommands(commandsBuilder.build())
                        .build();
        currentSpan.addEvent("built ledger submit request");

        return toCompletableFuture(submission.submit(request));
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
