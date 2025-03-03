// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.api.AppInstallsApi;
import com.digitalasset.quickstart.ledger.LedgerApi;
import com.digitalasset.quickstart.security.AuthenticatedPartyProvider;
import com.digitalasset.quickstart.repository.DamlRepository;
import com.digitalasset.transcode.java.ContractId;
import com.digitalasset.transcode.java.Party;
import quickstart_licensing.licensing.license.License;
import quickstart_licensing.licensing.license.LicenseParams;
import quickstart_licensing.licensing.util.Metadata;
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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class AppInstallsApiImpl implements AppInstallsApi {

    private final LedgerApi ledger;
    private final DamlRepository damlRepository;
    private final AuthenticatedPartyProvider authenticatedPartyProvider;
    private final Logger logger = LoggerFactory.getLogger(AppInstallsApiImpl.class);

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

    /**
     * List all AppInstall contracts visible to the authenticated party.
     */
    @Override
    public CompletableFuture<ResponseEntity<List<org.openapitools.model.AppInstall>>> listAppInstalls() {
        logger.info("Listing AppInstalls");
        String party = authenticatedPartyProvider.getPartyOrFail();

        return damlRepository.findActiveAppInstalls()
                .thenApply(contracts -> {
                    List<org.openapitools.model.AppInstall> result = contracts.stream()
                            .filter(contract -> {
                                // Filter to those relevant to the requesting party
                                String dso = contract.payload.getDso.getParty;
                                String provider = contract.payload.getProvider.getParty;
                                String user = contract.payload.getUser.getParty;
                                // The requesting party must be involved
                                return party.equals(dso) || party.equals(provider) || party.equals(user);
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
                })
                .exceptionally(ex -> {
                    logger.error("Error listing AppInstalls", ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }

    /**
     * Create a new License contract by exercising the AppInstall_CreateLicense choice.
     */
    @Override
    public CompletableFuture<ResponseEntity<AppInstallCreateLicenseResult>> createLicense(
            String contractId,
            String commandId,
            AppInstallCreateLicenseRequest createLicenseRequest
    ) {
        logger.info("Creating License from AppInstall with contractId: {}", contractId);
        String actorParty = authenticatedPartyProvider.getPartyOrFail();

        // Use DamlRepository instead of pqs.byContractId(...)
        return damlRepository.findAppInstallById(contractId).thenCompose(contract -> {
            // Ensure the actor party is the provider of this app install.
            String providerParty = contract.payload.getProvider.getParty;
            if (!actorParty.equals(providerParty)) {
                logger.error("Party {} is not the provider of this AppInstall", actorParty);
                return CompletableFuture.completedFuture(
                        ResponseEntity.status(HttpStatus.FORBIDDEN).build()
                );
            }

            // Convert the params meta data
            Metadata paramsMeta = new Metadata(createLicenseRequest.getParams().getMeta().getData());

            // Create choice instance
            LicenseParams params = new LicenseParams(paramsMeta);
            quickstart_licensing.licensing.appinstall.AppInstall.AppInstall_CreateLicense choice =
                    new quickstart_licensing.licensing.appinstall.AppInstall.AppInstall_CreateLicense(params);

            // Exercise the choice on the ledger
            return ledger.exerciseAndGetResult(actorParty, contract.contractId, choice, commandId)
                    .thenApply(licenseContractId -> {
                        // The result should be ContractId<License>
                        ContractId<License> licenseCid = licenseContractId.getLicenseId;

                        AppInstallCreateLicenseResult result = new AppInstallCreateLicenseResult();
                        result.setInstallId(contractId);
                        result.setLicenseId(licenseCid.getContractId);
                        return ResponseEntity.ok(result);
                    })
                    .exceptionally(ex -> {
                        logger.error("Error creating License from AppInstall", ex);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    });
        }).exceptionally(ex -> {
            logger.error("Error fetching AppInstall with contractId: {}", contractId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        });
    }

    /**
     * Cancel an AppInstall by exercising the AppInstall_Cancel choice.
     */
    @Override
    public CompletableFuture<ResponseEntity<Void>> cancelAppInstall(
            String contractId,
            String commandId,
            AppInstallCancel appInstallCancel
    ) {
        logger.info("Cancelling AppInstall with contractId: {}", contractId);
        String actorParty = authenticatedPartyProvider.getPartyOrFail();

        // Use DamlRepository instead of pqs.byContractId(...)
        return damlRepository.findAppInstallById(contractId).thenCompose(contract -> {
            // Check if the actor can cancel
            String user = contract.payload.getUser.getParty;
            if (!actorParty.equals(user)) {
                logger.error("Party {} is not the user of this AppInstall", actorParty);
                return CompletableFuture.completedFuture(
                        ResponseEntity.status(HttpStatus.FORBIDDEN).build()
                );
            }

            Metadata meta = new Metadata(appInstallCancel.getMeta().getData());
            Party actor = new Party(actorParty);

            // Create choice instance
            quickstart_licensing.licensing.appinstall.AppInstall.AppInstall_Cancel choice =
                    new quickstart_licensing.licensing.appinstall.AppInstall.AppInstall_Cancel(actor, meta);

            return ledger.exerciseAndGetResult(actorParty, contract.contractId, choice, commandId)
                    .thenApply(result -> {
                        // Choice returns Unit
                        return ResponseEntity.ok().<Void>build();
                    })
                    .exceptionally(ex -> {
                        logger.error("Error cancelling AppInstall", ex);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    });
        }).exceptionally(ex -> {
            logger.error("Error fetching AppInstall with contractId: {}", contractId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        });
    }
}
