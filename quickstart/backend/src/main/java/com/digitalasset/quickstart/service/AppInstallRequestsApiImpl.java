// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.api.AppInstallRequestsApi;
import com.digitalasset.quickstart.ledger.LedgerApi;
import com.digitalasset.quickstart.oauth.AuthenticatedPartyService;
import com.digitalasset.quickstart.repository.DamlRepository;
import org.openapitools.model.AppInstall;
import org.openapitools.model.AppInstallRequestAccept;
import org.openapitools.model.AppInstallRequestCancel;
import org.openapitools.model.AppInstallRequestReject;
import org.openapitools.model.AppInstallRequest;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.digitalasset.quickstart.utility.ContextAwareCompletableFutures.completeWithin;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class AppInstallRequestsApiImpl implements AppInstallRequestsApi {

    private static final Logger logger = LoggerFactory.getLogger(AppInstallRequestsApiImpl.class);

    private final LedgerApi ledger;
    private final AuthenticatedPartyService authenticatedPartyService;
    private final DamlRepository damlRepository;

    @Autowired
    public AppInstallRequestsApiImpl(
            LedgerApi ledger,
            AuthenticatedPartyService authenticatedPartyService,
            DamlRepository damlRepository
    ) {
        this.ledger = ledger;
        this.authenticatedPartyService = authenticatedPartyService;
        this.damlRepository = damlRepository;
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<AppInstall>> acceptAppInstallRequest(
            @SpanAttribute("appInstall.contractId") String contractId,
            @SpanAttribute("appInstall.commandId") String commandId,
            AppInstallRequestAccept appInstallRequestAccept
    ) {
        Span methodSpan = Span.current();
        Context parentContext = Context.current();

        methodSpan.addEvent("Starting acceptAppInstallRequest");
        methodSpan.setAttribute("contractId", contractId);
        methodSpan.setAttribute("commandId", commandId);
        logger.info("acceptAppInstallRequest: contractId='{}', commandId='{}'", contractId, commandId);

        // Capture the provider party asynchronously
        return authenticatedPartyService.getPartyOrFail()
                .thenCompose(providerParty ->
                        damlRepository.findAppInstallRequestById(contractId)
                                .thenCompose(contract -> {
                                    methodSpan.addEvent("Fetched contract, exercising AppInstallRequest_Accept choice");

                                    quickstart_licensing.licensing.appinstall.AppInstallRequest.AppInstallRequest_Accept choice =
                                            new quickstart_licensing.licensing.appinstall.AppInstallRequest.AppInstallRequest_Accept(
                                                    new quickstart_licensing.licensing.util.Metadata(appInstallRequestAccept.getInstallMeta().getData()),
                                                    new quickstart_licensing.licensing.util.Metadata(appInstallRequestAccept.getMeta().getData())
                                            );

                                    return ledger.exerciseAndGetResult(providerParty, contract.contractId, choice, commandId)
                                            .thenApply(appInstallContractId -> {
                                                methodSpan.addEvent("Choice exercised, building response AppInstall");
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
                                logger.info("acceptAppInstallRequest: Success for contractId='{}'", contractId);
                            } else {
                                logger.error("acceptAppInstallRequest: Failed for contractId='{}': {}", contractId, ex.getMessage(), ex);
                                methodSpan.recordException(ex);
                                methodSpan.setStatus(StatusCode.ERROR, ex.getMessage());
                            }
                        })
                );
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<Void>> cancelAppInstallRequest(
            @SpanAttribute("appInstall.contractId") String contractId,
            @SpanAttribute("appInstall.commandId") String commandId,
            AppInstallRequestCancel appInstallRequestCancel
    ) {
        Span methodSpan = Span.current();
        Context parentContext = Context.current();

        methodSpan.addEvent("Starting cancelAppInstallRequest");
        methodSpan.setAttribute("contractId", contractId);
        methodSpan.setAttribute("commandId", commandId);
        logger.info("cancelAppInstallRequest: contractId='{}', commandId='{}'", contractId, commandId);

        return authenticatedPartyService.getPartyOrFail()
                .thenCompose(userParty ->
                        damlRepository.findAppInstallRequestById(contractId)
                                .thenCompose(contract -> {
                                    methodSpan.addEvent("Fetched contract, exercising AppInstallRequest_Cancel choice");

                                    quickstart_licensing.licensing.appinstall.AppInstallRequest.AppInstallRequest_Cancel choice =
                                            new quickstart_licensing.licensing.appinstall.AppInstallRequest.AppInstallRequest_Cancel(
                                                    new quickstart_licensing.licensing.util.Metadata(appInstallRequestCancel.getMeta().getData())
                                            );

                                    return ledger.exerciseAndGetResult(userParty, contract.contractId, choice, commandId)
                                            .thenApply(result -> {
                                                methodSpan.addEvent("Choice exercised, returning 200 OK");
                                                return ResponseEntity.ok().<Void>build();
                                            });
                                })
                )
                .whenComplete(
                        completeWithin(parentContext, (res, ex) -> {
                            if (ex == null) {
                                logger.info("cancelAppInstallRequest: Success for contractId='{}'", contractId);
                            } else {
                                logger.error("cancelAppInstallRequest: Failed for contractId='{}': {}", contractId, ex.getMessage(), ex);
                                methodSpan.recordException(ex);
                                methodSpan.setStatus(StatusCode.ERROR, ex.getMessage());
                            }
                        })
                );
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<List<AppInstallRequest>>> listAppInstallRequests() {
        Span methodSpan = Span.current();
        Context parentContext = Context.current();

        methodSpan.addEvent("Starting listAppInstallRequests");
        logger.info("listAppInstallRequests: retrieving active requests");

        return authenticatedPartyService.getPartyOrFail()
                .thenCompose(party ->
                        damlRepository.findActiveAppInstallRequests()
                                .thenApply(contracts -> {
                                    methodSpan.addEvent("Filtering results by current party");

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
                                logger.info("listAppInstallRequests: Success, found {} records", count);
                            } else {
                                logger.error("listAppInstallRequests: Failed: {}", ex.getMessage(), ex);
                                methodSpan.recordException(ex);
                                methodSpan.setStatus(StatusCode.ERROR, ex.getMessage());
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
        Span methodSpan = Span.current();
        Context parentContext = Context.current();

        methodSpan.addEvent("Starting rejectAppInstallRequest");
        methodSpan.setAttribute("contractId", contractId);
        methodSpan.setAttribute("commandId", commandId);
        logger.info("rejectAppInstallRequest: contractId='{}', commandId='{}'", contractId, commandId);

        return authenticatedPartyService.getPartyOrFail()
                .thenCompose(providerParty ->
                        damlRepository.findAppInstallRequestById(contractId)
                                .thenCompose(contract -> {
                                    methodSpan.addEvent("Fetched contract, exercising AppInstallRequest_Reject choice");

                                    quickstart_licensing.licensing.appinstall.AppInstallRequest.AppInstallRequest_Reject choice =
                                            new quickstart_licensing.licensing.appinstall.AppInstallRequest.AppInstallRequest_Reject(
                                                    new quickstart_licensing.licensing.util.Metadata(appInstallRequestReject.getMeta().getData())
                                            );

                                    return ledger.exerciseAndGetResult(providerParty, contract.contractId, choice, commandId)
                                            .thenApply(result -> {
                                                methodSpan.addEvent("Choice exercised, returning 200 OK");
                                                return ResponseEntity.ok().<Void>build();
                                            });
                                })
                )
                .whenComplete(
                        completeWithin(parentContext, (res, ex) -> {
                            if (ex == null) {
                                logger.info("rejectAppInstallRequest: Success for contractId='{}'", contractId);
                            } else {
                                logger.error("rejectAppInstallRequest: Failed for contractId='{}': {}", contractId, ex.getMessage(), ex);
                                methodSpan.recordException(ex);
                                methodSpan.setStatus(StatusCode.ERROR, ex.getMessage());
                            }
                        })
                );
    }
}

