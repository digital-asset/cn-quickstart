// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.api.AppInstallRequestsApi;
import com.digitalasset.quickstart.ledger.LedgerApi;
import com.digitalasset.quickstart.security.AuthenticatedPartyProvider;
import com.digitalasset.quickstart.repository.DamlRepository;
import com.digitalasset.quickstart.utility.LoggingSpanHelper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.openapitools.model.AppInstall;
import org.openapitools.model.AppInstallRequest;
import org.openapitools.model.AppInstallRequestAccept;
import org.openapitools.model.AppInstallRequestCancel;
import org.openapitools.model.AppInstallRequestReject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import splice_api_token_metadata_v1.splice.api.token.metadatav1.Metadata;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.digitalasset.quickstart.utility.ContextAwareCompletableFutures.completeWithin;
import static quickstart_licensing.licensing.appinstall.AppInstallRequest.TEMPLATE_ID;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class AppInstallRequestsApiImpl implements AppInstallRequestsApi {

    private static final Logger logger = LoggerFactory.getLogger(AppInstallRequestsApiImpl.class);

    private final LedgerApi ledger;
    private final AuthenticatedPartyProvider authenticatedPartyProvider;
    private final DamlRepository damlRepository;

    @Autowired
    public AppInstallRequestsApiImpl(
            LedgerApi ledger,
            AuthenticatedPartyProvider authenticatedPartyProvider,
            DamlRepository damlRepository
    ) {
        this.ledger = ledger;
        this.authenticatedPartyProvider = authenticatedPartyProvider;
        this.damlRepository = damlRepository;
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<AppInstall>> acceptAppInstallRequest(
            @SpanAttribute("appInstall.contractId") String contractId,
            @SpanAttribute("appInstall.commandId") String commandId,
            AppInstallRequestAccept appInstallRequestAccept
    ) {
        Span span = Span.current();
        Context parentContext = Context.current();

        Map<String, Object> attributes = Map.of(
                "contractId", contractId,
                "commandId", commandId,
                "templateId", TEMPLATE_ID.qualifiedName(),
                "choiceName", "AppInstallRequest_Accept"
        );

        LoggingSpanHelper.addEventWithAttributes(span, "Starting acceptAppInstallRequest", attributes);
        LoggingSpanHelper.setSpanAttributes(span, attributes);
        LoggingSpanHelper.logInfo(logger, "acceptAppInstallRequest: received request", attributes);

        return CompletableFuture.completedFuture(authenticatedPartyProvider.getPartyOrFail())
                .thenCompose(providerParty ->
                        damlRepository.findAppInstallRequestById(contractId)
                                .thenCompose(contract -> {
                                    span.addEvent("Fetched contract, checking if request is already accepted");

                                    var choice = new quickstart_licensing.licensing.appinstall.AppInstallRequest.AppInstallRequest_Accept(
                                            new splice_api_token_metadata_v1.splice.api.token.metadatav1.Metadata(appInstallRequestAccept.getInstallMeta().getData()),
                                            new splice_api_token_metadata_v1.splice.api.token.metadatav1.Metadata(appInstallRequestAccept.getMeta().getData())
                                    );

                                    return ledger.exerciseAndGetResult(providerParty, contract.contractId, choice, commandId)
                                            .thenApply(appInstallContractId -> {
                                                span.addEvent("Choice exercised, building response AppInstall");
                                                AppInstall appInstall = new AppInstall();
                                                appInstall.setDso(contract.payload.getDso.getParty);
                                                appInstall.setProvider(contract.payload.getProvider.getParty);
                                                appInstall.setUser(contract.payload.getUser.getParty);
                                                appInstall.setMeta(appInstallRequestAccept.getInstallMeta());
                                                appInstall.setNumLicensesCreated(0);
                                                return ResponseEntity.ok(appInstall);
                                            });
                                })
                )
                .whenComplete(
                        completeWithin(parentContext, (res, ex) -> {
                            if (ex == null) {
                                LoggingSpanHelper.logDebug(logger, "acceptAppInstallRequest: success", attributes);
                            } else {
                                LoggingSpanHelper.logError(logger, "acceptAppInstallRequest: failed", attributes, ex);
                                LoggingSpanHelper.recordException(span, ex);
                            }
                        })
                );
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<List<AppInstallRequest>>> listAppInstallRequests() {
        Span span = Span.current();
        Context parentContext = Context.current();

        Map<String, Object> attributes = Map.of(
                "templateId", TEMPLATE_ID.qualifiedName()
        );

        LoggingSpanHelper.addEventWithAttributes(span, "Starting listAppInstallRequests", attributes);
        LoggingSpanHelper.setSpanAttributes(span, attributes);
        LoggingSpanHelper.logInfo(logger, "listAppInstallRequests: received request, retrieving active requests", attributes);

        return CompletableFuture.completedFuture(authenticatedPartyProvider.getPartyOrFail())
                .thenCompose(party ->
                        damlRepository.findActiveAppInstallRequests()
                                .thenApply(contracts -> {
                                    span.addEvent("Fetched active requests, filtering by current party");

                                    List<AppInstallRequest> result = contracts.stream()
                                            .filter(contract -> {
                                                String user = contract.payload.getUser.getParty;
                                                String provider = contract.payload.getProvider.getParty;
                                                return party.equals(user) || party.equals(provider);
                                            })
                                            .map(contract -> {
                                                AppInstallRequest appInstallRequest = new AppInstallRequest();
                                                appInstallRequest.setContractId(contract.contractId.getContractId);
                                                appInstallRequest.setDso(contract.payload.getDso.getParty);
                                                appInstallRequest.setProvider(contract.payload.getProvider.getParty);
                                                appInstallRequest.setUser(contract.payload.getUser.getParty);
                                                appInstallRequest.setMeta(new org.openapitools.model.Metadata());
                                                appInstallRequest.getMeta().setData(contract.payload.getMeta.getValues);
                                                return appInstallRequest;
                                            })
                                            .toList();
                                    return ResponseEntity.ok(result);
                                })
                )
                .whenComplete(
                        completeWithin(parentContext, (res, ex) -> {
                            if (ex == null) {
                                int count = (res.getBody() == null) ? 0 : res.getBody().size();
                                logger.atDebug().addKeyValue("recordCount", count).log("listAppInstallRequests: success");
                            } else {
                                LoggingSpanHelper.logError(logger, "listAppInstallRequests: failed", attributes, ex);
                                LoggingSpanHelper.recordException(span, ex);
                            }
                        })
                );
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<Void>> rejectAppInstallRequest(
            @SpanAttribute("appInstall.contractId") String contractId,
            @SpanAttribute("appInstall.commandId") String commandId,
            AppInstallRequestReject appInstallRequestReject
    ) {
        Span span = Span.current();
        Context parentContext = Context.current();

        Map<String, Object> attributes = Map.of(
                "contractId", contractId,
                "commandId", commandId,
                "templateId", TEMPLATE_ID.qualifiedName(),
                "choiceName", "AppInstallRequest_Reject"
        );

        LoggingSpanHelper.addEventWithAttributes(span, "Starting rejectAppInstallRequest", attributes);
        LoggingSpanHelper.setSpanAttributes(span, attributes);
        LoggingSpanHelper.logInfo(logger, "rejectAppInstallRequest: received request", attributes);

        return CompletableFuture.completedFuture(authenticatedPartyProvider.getPartyOrFail())
                .thenCompose(providerParty ->
                        damlRepository.findAppInstallRequestById(contractId)
                                .thenCompose(contract -> {
                                    span.addEvent("Fetched contract, exercising AppInstallRequest_Reject choice");

                                    var choice = new quickstart_licensing.licensing.appinstall.AppInstallRequest.AppInstallRequest_Reject(
                                            new Metadata(appInstallRequestReject.getMeta().getData())
                                    );

                                    return ledger.exerciseAndGetResult(providerParty, contract.contractId, choice, commandId)
                                            .thenApply(result -> {
                                                span.addEvent("Choice exercised, returning 200 OK");
                                                return ResponseEntity.ok().<Void>build();
                                            });
                                })
                )
                .whenComplete(
                        completeWithin(parentContext, (res, ex) -> {
                            if (ex == null) {
                                LoggingSpanHelper.logDebug(logger, "rejectAppInstallRequest: success", attributes);
                            } else {
                                LoggingSpanHelper.logError(logger, "rejectAppInstallRequest: failed", attributes, ex);
                                LoggingSpanHelper.recordException(span, ex);
                            }
                        })
                );
    }
}
