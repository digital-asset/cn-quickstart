// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import static com.digitalasset.quickstart.service.ServiceUtils.ensurePresent;
import static com.digitalasset.quickstart.service.ServiceUtils.traceServiceCallAsync;
import static com.digitalasset.quickstart.utility.TracingUtils.tracingCtx;

import com.digitalasset.quickstart.api.AppInstallRequestsApi;
import com.digitalasset.quickstart.ledger.LedgerApi;
import com.digitalasset.quickstart.repository.DamlRepository;
import com.digitalasset.quickstart.security.AuthUtils;
import io.opentelemetry.instrumentation.annotations.WithSpan;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.openapitools.model.AppInstall;
import org.openapitools.model.AppInstallRequest;
import org.openapitools.model.AppInstallRequestAccept;
import org.openapitools.model.AppInstallRequestReject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import quickstart_licensing.licensing.appinstall.AppInstallRequest.AppInstallRequest_Reject;
import splice_api_token_metadata_v1.splice.api.token.metadatav1.Metadata;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class AppInstallRequestsApiImpl implements AppInstallRequestsApi {

    private static final Logger logger = LoggerFactory.getLogger(AppInstallRequestsApiImpl.class);

    private final LedgerApi ledger;
    private final AuthUtils auth;
    private final DamlRepository damlRepository;

    @Autowired
    public AppInstallRequestsApiImpl(
            LedgerApi ledger,
            AuthUtils auth,
            DamlRepository damlRepository
    ) {
        this.ledger = ledger;
        this.auth = auth;
        this.damlRepository = damlRepository;
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<AppInstall>> acceptAppInstallRequest(
            String contractId,
            String commandId,
            AppInstallRequestAccept appInstallRequestAccept
    ) {
        var ctx = tracingCtx(logger, "acceptAppInstallRequest",
                "contractId", contractId,
                "commandId", commandId
        );
        return auth.asAdminParty(party -> traceServiceCallAsync(ctx, () ->
                damlRepository.findAppInstallRequestById(contractId).thenComposeAsync(optContract -> {
                    var contract = ensurePresent(optContract, "AppInstallRequest not found for contract %s", contractId);
                    var choice = new quickstart_licensing.licensing.appinstall.AppInstallRequest.AppInstallRequest_Accept(
                            new splice_api_token_metadata_v1.splice.api.token.metadatav1.Metadata(
                                    appInstallRequestAccept.getInstallMeta().getData()),
                            new splice_api_token_metadata_v1.splice.api.token.metadatav1.Metadata(
                                    appInstallRequestAccept.getMeta().getData()));

                    return ledger.exerciseAndGetResult(contract.contractId, choice, commandId)
                            .thenApply(appInstallContractId -> {
                                AppInstall appInstall = new AppInstall();
                                appInstall.setProvider(contract.payload.getProvider.getParty);
                                appInstall.setUser(contract.payload.getUser.getParty);
                                appInstall.setMeta(appInstallRequestAccept.getInstallMeta());
                                appInstall.setNumLicensesCreated(0);
                                return ResponseEntity.status(HttpStatus.CREATED).body(appInstall);
                            });
                })
        ));
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<List<AppInstallRequest>>> listAppInstallRequests() {
        var ctx = tracingCtx(logger, "listAppInstallRequests");
        return auth.asAuthenticatedParty(party -> traceServiceCallAsync(ctx, () ->
                damlRepository.findActiveAppInstallRequests().thenApplyAsync(contracts -> {
                    List<AppInstallRequest> result = contracts.stream().filter(contract -> {
                        String user = contract.payload.getUser.getParty;
                        String provider = contract.payload.getProvider.getParty;
                        return party.equals(user) || party.equals(provider);
                    }).map(contract -> {
                        AppInstallRequest appInstallRequest = new AppInstallRequest();
                        appInstallRequest.setContractId(contract.contractId.getContractId);
                        appInstallRequest.setProvider(contract.payload.getProvider.getParty);
                        appInstallRequest.setUser(contract.payload.getUser.getParty);
                        appInstallRequest.setMeta(new org.openapitools.model.Metadata());
                        appInstallRequest.getMeta().setData(contract.payload.getMeta.getValues);
                        return appInstallRequest;
                    }).toList();
                    return ResponseEntity.ok(result);
                })
        ));
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<Void>> rejectAppInstallRequest(
            String contractId,
            String commandId,
            AppInstallRequestReject appInstallRequestReject
    ) {
        var ctx = tracingCtx(logger, "rejectAppInstallRequest",
                "contractId", contractId,
                "commandId", commandId
        );
        return auth.asAdminParty(party -> traceServiceCallAsync(ctx, () ->
                damlRepository.findAppInstallRequestById(contractId)
                    .thenComposeAsync(optContract -> {
                        var contract = ensurePresent(optContract, "AppInstallRequest not found for contract %s", contractId);
                        var choice = new AppInstallRequest_Reject(new Metadata(appInstallRequestReject.getMeta().getData()));
                        return ledger.exerciseAndGetResult(contract.contractId, choice, commandId)
                                   .thenApply(result -> ResponseEntity.noContent().build());
                })
        ));
    }
}
