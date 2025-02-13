// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.api.AppInstallsApi;
import com.digitalasset.quickstart.ledger.LedgerApi;
import com.digitalasset.quickstart.oauth.AuthenticatedPartyService;
import com.digitalasset.quickstart.repository.DamlRepository;
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
import org.openapitools.model.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import quickstart_licensing.licensing.license.License;
import quickstart_licensing.licensing.license.LicenseParams;
import quickstart_licensing.licensing.appinstall.AppInstall.AppInstall_Cancel;
import quickstart_licensing.licensing.appinstall.AppInstall.AppInstall_CreateLicense;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.digitalasset.quickstart.utility.ContextAwareCompletableFutures.completeWithin;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class AppInstallsApiImpl implements AppInstallsApi {

    private final LedgerApi ledger;
    private final DamlRepository damlRepository;
    private final AuthenticatedPartyService authenticatedPartyService;
    private static final Logger logger = LoggerFactory.getLogger(AppInstallsApiImpl.class);

    @Autowired
    public AppInstallsApiImpl(
            LedgerApi ledger,
            DamlRepository damlRepository,
            AuthenticatedPartyService authenticatedPartyService
    ) {
        this.ledger = ledger;
        this.damlRepository = damlRepository;
        this.authenticatedPartyService = authenticatedPartyService;
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<List<org.openapitools.model.AppInstall>>> listAppInstalls() {
        Span methodSpan = Span.current();
        Context parentContext = Context.current();

        methodSpan.addEvent("Starting listAppInstalls");
        logger.info("listAppInstalls: retrieving AppInstalls for the requesting party");

        // Now do it asynchronously
        return authenticatedPartyService.getPartyOrFail()
                .thenCompose(requestingParty -> {
                    methodSpan.setAttribute("requesting.party", requestingParty);

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
                                logger.info("listAppInstalls: success, found {} records", count);
                            } else {
                                logger.error("listAppInstalls: failed: {}", ex.getMessage(), ex);
                                methodSpan.recordException(ex);
                                methodSpan.setStatus(StatusCode.ERROR, ex.getMessage());
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

        methodSpan.addEvent("Starting createLicense");
        methodSpan.setAttribute("contractId", contractId);
        methodSpan.setAttribute("commandId", commandId);
        logger.info("createLicense: contractId='{}', commandId='{}'", contractId, commandId);

        return authenticatedPartyService.getPartyOrFail()
                .thenCompose(actorParty -> {
                    // Now fetch the contract
                    return damlRepository.findAppInstallById(contractId)
                            .thenCompose(contract -> {
                                methodSpan.addEvent("Fetched contract, verifying provider");
                                String providerParty = contract.payload.getProvider.getParty;

                                if (!actorParty.equals(providerParty)) {
                                    logger.error("createLicense: party='{}' is not the provider for contractId='{}'",
                                            actorParty, contractId);
                                    return CompletableFuture.completedFuture(
                                            ResponseEntity.status(HttpStatus.FORBIDDEN).build());
                                }

                                quickstart_licensing.licensing.util.Metadata paramsMeta =
                                        new quickstart_licensing.licensing.util.Metadata(
                                                createLicenseRequest.getParams().getMeta().getData());
                                LicenseParams params = new LicenseParams(paramsMeta);
                                AppInstall_CreateLicense choice = new AppInstall_CreateLicense(params);

                                return ledger.exerciseAndGetResult(actorParty, contract.contractId, choice, commandId)
                                        .<ResponseEntity<AppInstallCreateLicenseResult>>thenApply(licenseContractId -> {
                                            methodSpan.addEvent("Choice exercised, building response");
                                            AppInstallCreateLicenseResult result = new AppInstallCreateLicenseResult();
                                            result.setInstallId(contractId);
                                            result.setLicenseId(licenseContractId.getLicenseId.getContractId);
                                            return ResponseEntity.ok(result);
                                        });
                            });
                })
                .whenComplete(
                        completeWithin(parentContext, (res, ex) -> {
                            if (ex == null) {
                                logger.info("createLicense: success for contractId='{}'", contractId);
                            } else {
                                logger.error("createLicense: failed for contractId='{}': {}", contractId, ex.getMessage(), ex);
                                methodSpan.recordException(ex);
                                methodSpan.setStatus(StatusCode.ERROR, ex.getMessage());
                            }
                        })
                );
    }


    /**
     * Cancel an AppInstall by exercising the AppInstall_Cancel choice.
     */
    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<Void>> cancelAppInstall(
            @SpanAttribute("appInstall.contractId") String contractId,
            @SpanAttribute("appInstall.commandId") String commandId,
            AppInstallCancel appInstallCancel
    ) {
        Span methodSpan = Span.current();
        Context parentContext = Context.current();

        methodSpan.addEvent("Starting cancelAppInstall");
        methodSpan.setAttribute("contractId", contractId);
        methodSpan.setAttribute("commandId", commandId);
        logger.info("cancelAppInstall: contractId='{}', commandId='{}'", contractId, commandId);

        // Capture the actor party asynchronously
        return authenticatedPartyService.getPartyOrFail()
                .thenCompose(actorParty ->
                        damlRepository.findAppInstallById(contractId)
                                .thenCompose(contract -> {
                                    methodSpan.addEvent("Fetched contract, verifying user");
                                    String userParty = contract.payload.getUser.getParty;

                                    // If the actor is not the user, return 403
                                    if (!actorParty.equals(userParty)) {
                                        logger.error("cancelAppInstall: party='{}' is not the user for contractId='{}'",
                                                actorParty, contractId);
                                        return CompletableFuture.completedFuture(
                                                ResponseEntity.status(HttpStatus.FORBIDDEN).build()
                                        );
                                    }

                                    // Construct the choice
                                    methodSpan.addEvent("Constructing AppInstall_Cancel choice");
                                    quickstart_licensing.licensing.util.Metadata meta =
                                            new quickstart_licensing.licensing.util.Metadata(appInstallCancel.getMeta().getData());
                                    Party actor = new Party(actorParty);

                                    AppInstall_Cancel choice = new AppInstall_Cancel(actor, meta);

                                    // Exercise the choice
                                    return ledger.exerciseAndGetResult(actorParty, contract.contractId, choice, commandId)
                                            .<ResponseEntity<Void>>thenApply(result -> {
                                                methodSpan.addEvent("Choice exercised, returning 200 OK");
                                                return ResponseEntity.ok().build();
                                            });
                                })
                )
                .whenComplete(
                        completeWithin(parentContext, (res, ex) -> {
                            if (ex == null) {
                                logger.info("cancelAppInstall: success for contractId='{}'", contractId);
                            } else {
                                logger.error("cancelAppInstall: failed for contractId='{}': {}", contractId, ex.getMessage(), ex);
                                methodSpan.recordException(ex);
                                methodSpan.setStatus(StatusCode.ERROR, ex.getMessage());
                            }
                        })
                );
    }
}
