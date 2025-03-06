// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.api.AppInstallsApi;
import com.digitalasset.quickstart.ledger.LedgerApi;
import com.digitalasset.quickstart.security.AuthenticatedPartyProvider;
import com.digitalasset.quickstart.repository.DamlRepository;
import com.digitalasset.quickstart.utility.LoggingSpanHelper;
import com.digitalasset.transcode.java.ContractId;
import com.digitalasset.transcode.java.Party;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.openapitools.model.AppInstallCancel;
import org.openapitools.model.AppInstallCreateLicenseRequest;
import org.openapitools.model.AppInstallCreateLicenseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import quickstart_licensing.licensing.appinstall.AppInstall.AppInstall_Cancel;
import quickstart_licensing.licensing.appinstall.AppInstall.AppInstall_CreateLicense;
import quickstart_licensing.licensing.license.LicenseParams;
import quickstart_licensing.licensing.util.Metadata;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.digitalasset.quickstart.utility.ContextAwareCompletableFutures.completeWithin;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class AppInstallsApiImpl implements AppInstallsApi {

    private final LedgerApi ledger;
    private final DamlRepository damlRepository;
    private final AuthenticatedPartyProvider authenticatedPartyProvider;
    private static final Logger logger = LoggerFactory.getLogger(AppInstallsApiImpl.class);

    @Autowired
    public AppInstallsApiImpl(
            LedgerApi ledger,
            DamlRepository damlRepository,
            AuthenticatedPartyProvider authenticatedPartyProvider
    ) {
        this.ledger = ledger;
        this.damlRepository = damlRepository;
        this.authenticatedPartyProvider = authenticatedPartyProvider;
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<List<org.openapitools.model.AppInstall>>> listAppInstalls() {
        Span methodSpan = Span.current();
        Context parentContext = Context.current();

        LoggingSpanHelper.addEventWithAttributes(methodSpan, "Starting listAppInstalls", null);
        LoggingSpanHelper.logInfo(logger, "listAppInstalls: retrieving AppInstalls for the requesting party");

        return authenticatedPartyService.getPartyOrFail()
                .thenCompose(requestingParty -> {
                    Map<String, Object> attrs = Map.of("requesting.party", requestingParty);
                    LoggingSpanHelper.setSpanAttributes(methodSpan, attrs);

                    return damlRepository.findActiveAppInstalls()
                            .thenApply(contracts -> {
                                methodSpan.addEvent("Filtering results by requesting party");
                                List<org.openapitools.model.AppInstall> result = contracts.stream()
                                        .filter(contract -> {
                                            String dso = contract.payload.getDso.getParty;
                                            String provider = contract.payload.getProvider.getParty;
                                            String user = contract.payload.getUser.getParty;
                                            return requestingParty.equals(dso)
                                                    || requestingParty.equals(provider)
                                                    || requestingParty.equals(user);
                                        })
                                        .map(contract -> {
                                            org.openapitools.model.AppInstall model = new org.openapitools.model.AppInstall();
                                            model.setContractId(contract.contractId.getContractId);
                                            model.setDso(contract.payload.getDso.getParty);
                                            model.setProvider(contract.payload.getProvider.getParty);
                                            model.setUser(contract.payload.getUser.getParty);

                                            org.openapitools.model.Metadata metaModel = new org.openapitools.model.Metadata();
                                            metaModel.setData(contract.payload.getMeta.getValues);
                                            model.setMeta(metaModel);

                                            model.setNumLicensesCreated(contract.payload.getNumLicensesCreated.intValue());
                                            return model;
                                        })
                                        .collect(Collectors.toList());

                                return ResponseEntity.ok(result);
                            });
                })
                .whenComplete(
                        completeWithin(parentContext, (res, ex) -> {
                            if (ex == null) {
                                int count = (res.getBody() != null) ? res.getBody().size() : 0;
                                Map<String, Object> successAttrs = Map.of("count", count);
                                LoggingSpanHelper.logInfo(logger, "listAppInstalls: success", successAttrs);
                            } else {
                                LoggingSpanHelper.logError(logger, "listAppInstalls: failed", null, ex);
                                LoggingSpanHelper.recordException(methodSpan, ex);
                            }
                        })
                );
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<AppInstallCreateLicenseResult>> createLicense(
            @SpanAttribute("appInstall.contractId") String contractId,
            @SpanAttribute("appInstall.commandId") String commandId,
            AppInstallCreateLicenseRequest createLicenseRequest
    ) {
        Span methodSpan = Span.current();
        Context parentContext = Context.current();

        Map<String, Object> startAttrs = Map.of("contractId", contractId, "commandId", commandId);
        LoggingSpanHelper.addEventWithAttributes(methodSpan, "Starting createLicense", startAttrs);
        LoggingSpanHelper.setSpanAttributes(methodSpan, startAttrs);
        LoggingSpanHelper.logInfo(logger, "createLicense: received request", startAttrs);

        return authenticatedPartyService.getPartyOrFail()
                .<ResponseEntity<AppInstallCreateLicenseResult>>thenCompose(actorParty -> damlRepository.findAppInstallById(contractId)
                        .thenCompose(contract -> {
                            methodSpan.addEvent("Fetched contract, verifying provider");
                            String providerParty = contract.payload.getProvider.getParty;

                            if (!actorParty.equals(providerParty)) {
                                Map<String, Object> errorAttrs = Map.of(
                                        "contractId", contractId,
                                        "commandId", commandId,
                                        "actorParty", actorParty
                                );
                                LoggingSpanHelper.logError(logger, "createLicense: party is not the provider", errorAttrs, null);
                                return CompletableFuture.completedFuture(
                                        ResponseEntity.status(HttpStatus.FORBIDDEN).build()
                                );
                            }

                            Metadata paramsMeta = new Metadata(createLicenseRequest.getParams().getMeta().getData());
                            LicenseParams params = new LicenseParams(paramsMeta);
                            AppInstall_CreateLicense choice = new AppInstall_CreateLicense(params);

                            return ledger.exerciseAndGetResult(actorParty, contract.contractId, choice, commandId)
                                    .thenApply(licenseContractId -> {
                                        methodSpan.addEvent("Choice exercised, building response");
                                        AppInstallCreateLicenseResult result = new AppInstallCreateLicenseResult();
                                        result.setInstallId(contractId);
                                        result.setLicenseId(licenseContractId.getLicenseId.getContractId);
                                        return ResponseEntity.ok(result);
                                    });
                        })
                )
                .whenComplete(
                        completeWithin(parentContext, (res, ex) -> {
                            if (ex == null) {
                                Map<String, Object> successAttrs = Map.of(
                                        "contractId", contractId,
                                        "commandId", commandId
                                );
                                LoggingSpanHelper.logDebug(logger, "createLicense: success", successAttrs);
                            } else {
                                Map<String, Object> failAttrs = Map.of("contractId", contractId, "commandId", commandId);
                                LoggingSpanHelper.logError(logger, "createLicense: failed", failAttrs, ex);
                                LoggingSpanHelper.recordException(methodSpan, ex);
                            }
                        })
                );
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<Void>> cancelAppInstall(
            @SpanAttribute("appInstall.contractId") String contractId,
            @SpanAttribute("appInstall.commandId") String commandId,
            AppInstallCancel appInstallCancel
    ) {
        Span methodSpan = Span.current();
        Context parentContext = Context.current();

        Map<String, Object> startAttrs = Map.of("contractId", contractId, "commandId", commandId);
        LoggingSpanHelper.addEventWithAttributes(methodSpan, "Starting cancelAppInstall", startAttrs);
        LoggingSpanHelper.setSpanAttributes(methodSpan, startAttrs);
        LoggingSpanHelper.logInfo(logger, "cancelAppInstall: received request", startAttrs);

        return authenticatedPartyService.getPartyOrFail()
                .<ResponseEntity<Void>>thenCompose(actorParty ->
                        damlRepository.findAppInstallById(contractId)
                                .thenCompose(contract -> {
                                    methodSpan.addEvent("Fetched contract, verifying user");
                                    String userParty = contract.payload.getUser.getParty;

                                    if (!actorParty.equals(userParty)) {
                                        Map<String, Object> errorAttrs = Map.of(
                                                "contractId", contractId,
                                                "commandId", commandId,
                                                "actorParty", actorParty
                                        );
                                        LoggingSpanHelper.logError(logger, "cancelAppInstall: party is not the user", errorAttrs, null);
                                        return CompletableFuture.completedFuture(
                                                ResponseEntity.status(HttpStatus.FORBIDDEN).build()
                                        );
                                    }

                                    methodSpan.addEvent("Constructing AppInstall_Cancel choice");
                                    Metadata meta = new Metadata(appInstallCancel.getMeta().getData());
                                    Party actor = new Party(actorParty);
                                    AppInstall_Cancel choice = new AppInstall_Cancel(actor, meta);

                                    return ledger.exerciseAndGetResult(actorParty, contract.contractId, choice, commandId)
                                            .thenApply(result -> {
                                                methodSpan.addEvent("Choice exercised, returning 200 OK");
                                                return ResponseEntity.ok().build();
                                            });
                                })
                )
                .whenComplete(
                        completeWithin(parentContext, (res, ex) -> {
                            if (ex == null) {
                                Map<String, Object> successAttrs = Map.of(
                                        "contractId", contractId,
                                        "commandId", commandId
                                );
                                LoggingSpanHelper.logDebug(logger, "cancelAppInstall: success", successAttrs);
                            } else {
                                Map<String, Object> failAttrs = Map.of("contractId", contractId, "commandId", commandId);
                                LoggingSpanHelper.logError(logger, "cancelAppInstall: failed", failAttrs, ex);
                                LoggingSpanHelper.recordException(methodSpan, ex);
                            }
                        })
                );
    }
}
