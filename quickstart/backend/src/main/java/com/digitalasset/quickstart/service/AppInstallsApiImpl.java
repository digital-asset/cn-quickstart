// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import static com.digitalasset.quickstart.service.ServiceUtils.traceServiceCallAsync;
import static com.digitalasset.quickstart.utility.TracingUtils.tracingCtx;

import com.digitalasset.quickstart.api.AppInstallsApi;
import com.digitalasset.quickstart.ledger.LedgerApi;
import com.digitalasset.quickstart.repository.DamlRepository;
import com.digitalasset.quickstart.security.AuthUtils;
import com.digitalasset.quickstart.security.AuthenticatedPartyProvider;
import com.digitalasset.transcode.java.Party;
import io.opentelemetry.instrumentation.annotations.WithSpan;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
import splice_api_token_metadata_v1.splice.api.token.metadatav1.Metadata;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class AppInstallsApiImpl implements AppInstallsApi {

    private final LedgerApi ledger;
    private final DamlRepository damlRepository;
    private final AuthUtils auth;
    private final AuthenticatedPartyProvider authenticatedPartyProvider;

    private static final Logger logger = LoggerFactory.getLogger(AppInstallsApiImpl.class);

    @Autowired
    public AppInstallsApiImpl(LedgerApi ledger, DamlRepository damlRepository, AuthUtils auth,
                              AuthenticatedPartyProvider authenticatedPartyProvider) {
        this.ledger = ledger;
        this.damlRepository = damlRepository;
        this.auth = auth;
        this.authenticatedPartyProvider = authenticatedPartyProvider;
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<List<org.openapitools.model.AppInstall>>> listAppInstalls() {
        var ctx = tracingCtx(logger, "listAppInstalls");
        return auth.asAuthenticatedParty(party -> traceServiceCallAsync(ctx, () ->
                damlRepository.findActiveAppInstalls().thenApply(contracts -> {
                    List<org.openapitools.model.AppInstall> result = contracts.stream().filter(contract -> {
                        String provider = contract.payload.getProvider.getParty;
                        String user = contract.payload.getUser.getParty;
                        return party.equals(provider) || party.equals(user);
                    }).map(contract -> {
                        org.openapitools.model.AppInstall model = new org.openapitools.model.AppInstall();
                        model.setContractId(contract.contractId.getContractId);
                        model.setProvider(contract.payload.getProvider.getParty);
                        model.setUser(contract.payload.getUser.getParty);

                        org.openapitools.model.Metadata metaModel = new org.openapitools.model.Metadata();
                        metaModel.setData(contract.payload.getMeta.getValues);
                        model.setMeta(metaModel);

                        model.setNumLicensesCreated(contract.payload.getNumLicensesCreated.intValue());
                        return model;
                    }).collect(Collectors.toList());
                    return ResponseEntity.ok(result);
                })
        ));
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<AppInstallCreateLicenseResult>> createLicense(
            String contractId,
            String commandId,
            AppInstallCreateLicenseRequest createLicenseRequest
    ) {
        var ctx = tracingCtx(logger, "createLicense",
                "contractId", contractId,
                "commandId", commandId
        );
        return auth.asAdminParty(party -> traceServiceCallAsync(ctx, () ->
                damlRepository.findAppInstallById(contractId).thenCompose(contract -> {
                    String providerParty = contract.payload.getProvider.getParty;
                    if (!party.equals(providerParty)) { // KV check this
                        throw new ServiceException(HttpStatus.FORBIDDEN, "Insufficient permissions");
                    }
                    Metadata paramsMeta = new Metadata(createLicenseRequest.getParams().getMeta().getData());
                    LicenseParams params = new LicenseParams(paramsMeta);
                    AppInstall_CreateLicense choice = new AppInstall_CreateLicense(params);
                    return ledger.exerciseAndGetResult(contract.contractId, choice, commandId)
                            .thenApply(licenseContractId -> {
                                AppInstallCreateLicenseResult result = new AppInstallCreateLicenseResult();
                                result.setInstallId(contractId);
                                result.setLicenseId(licenseContractId.getLicenseId.getContractId);
                                return ResponseEntity.ok(result);
                            });
                })
        ));
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<Void>> cancelAppInstall(
            String contractId,
            String commandId,
            AppInstallCancel appInstallCancel
    ) {
        var ctx = tracingCtx(logger, "cancelAppInstall",
                "contractId", contractId,
                "commandId", commandId
        );
        return auth.asAuthenticatedParty(party -> traceServiceCallAsync(ctx, () ->
                damlRepository.findAppInstallById(contractId).thenCompose(contract -> {
                    String userParty = contract.payload.getUser.getParty;
                    if (!party.equals(userParty)
                            && !party.equals(contract.payload.getProvider.getParty)) {
                        throw new ServiceException(HttpStatus.FORBIDDEN, "party {} is not the user nor provider", party);
                    }
                    Metadata meta = new Metadata(appInstallCancel.getMeta().getData());
                    // topologically we can only act as the provider
                    Party provider = new Party(auth.getAppProviderPartyId());
                    AppInstall_Cancel choice = new AppInstall_Cancel(provider, meta);
                    return ledger.exerciseAndGetResult(contract.contractId, choice, commandId)
                            .thenApply(result -> ResponseEntity.ok().build());
                })
        ));
    }
}
