// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.api.AppInstallRequestsApi;
import com.digitalasset.quickstart.ledger.LedgerApi;
import com.digitalasset.quickstart.oauth.AuthenticatedPartyProvider;
import com.digitalasset.quickstart.repository.DamlRepository;
import org.openapitools.model.AppInstall;
import org.openapitools.model.AppInstallRequestAccept;
import org.openapitools.model.AppInstallRequestCancel;
import org.openapitools.model.AppInstallRequestReject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class AppInstallRequestsApiImpl implements AppInstallRequestsApi {
    private final LedgerApi ledger;
    private final AuthenticatedPartyProvider authenticatedPartyProvider;
    private final Logger logger = LoggerFactory.getLogger(AppInstallRequestsApiImpl.class);
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
    public CompletableFuture<ResponseEntity<AppInstall>> acceptAppInstallRequest(
            String contractId,
            String commandId,
            AppInstallRequestAccept appInstallRequestAccept
    ) {
        logger.info("Accepting AppInstallRequest with contractId: {}", contractId);
        String providerParty = authenticatedPartyProvider.getPartyOrFail();
        return damlRepository.findAppInstallRequestById(contractId).thenCompose(contract -> {
            // Create choice instance
            quickstart_licensing.licensing.appinstall.AppInstallRequest.AppInstallRequest_Accept choice =
                    new quickstart_licensing.licensing.appinstall.AppInstallRequest.AppInstallRequest_Accept(
                            new quickstart_licensing.licensing.util.Metadata(appInstallRequestAccept.getInstallMeta().getData()),
                            new quickstart_licensing.licensing.util.Metadata(appInstallRequestAccept.getMeta().getData())
                    );

            return ledger.exerciseAndGetResult(providerParty, contract.contractId, choice, commandId)
                    .thenApply(appInstallContractId -> {
                        // Build response AppInstall object
                        AppInstall appInstallResponse = new AppInstall();
                        appInstallResponse.setDso(contract.payload.getDso.getParty);
                        appInstallResponse.setProvider(contract.payload.getProvider.getParty);
                        appInstallResponse.setUser(contract.payload.getUser.getParty);
                        // Set meta to the installMeta provided in the choice
                        appInstallResponse.setMeta(appInstallRequestAccept.getInstallMeta());
                        appInstallResponse.setNumLicensesCreated(0);

                        return ResponseEntity.ok(appInstallResponse);
                    }).exceptionally(ex -> {
                        logger.error("Error accepting AppInstallRequest", ex);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    });
        }).exceptionally(ex -> {
            logger.error("Error fetching AppInstallRequest with contractId: {}", contractId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        });
    }

    @Override
    public CompletableFuture<ResponseEntity<Void>> cancelAppInstallRequest(
            String contractId,
            String commandId,
            AppInstallRequestCancel appInstallRequestCancel
    ) {
        logger.info("Cancelling AppInstallRequest with contractId: {}", contractId);
        String userParty = authenticatedPartyProvider.getPartyOrFail();
        return damlRepository.findAppInstallRequestById(contractId).thenCompose(contract -> {
            // Create choice instance
            quickstart_licensing.licensing.appinstall.AppInstallRequest.AppInstallRequest_Cancel choice =
                    new quickstart_licensing.licensing.appinstall.AppInstallRequest.AppInstallRequest_Cancel(
                            new quickstart_licensing.licensing.util.Metadata(appInstallRequestCancel.getMeta().getData())
                    );

            return ledger.exerciseAndGetResult(userParty, contract.contractId, choice, commandId)
                    .thenApply(result -> {
                        // Choice returns Unit, so we can return 200 OK
                        return ResponseEntity.ok().<Void>build();
                    }).exceptionally(ex -> {
                        logger.error("Error cancelling AppInstallRequest", ex);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    });
        }).exceptionally(ex -> {
            logger.error("Error fetching AppInstallRequest with contractId: {}", contractId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        });
    }

    @Override
    public CompletableFuture<ResponseEntity<List<org.openapitools.model.AppInstallRequest>>> listAppInstallRequests() {
        logger.info("Listing AppInstallRequests");
        String party = authenticatedPartyProvider.getPartyOrFail();
        // Use damlRepository instead of pqs.active
        return damlRepository.findActiveAppInstallRequests().thenApply(contracts -> {
            List<org.openapitools.model.AppInstallRequest> result = contracts.stream()
                    .filter(contract -> {
                        // Filter contracts where the party is involved (signatory or observer)
                        String user = contract.payload.getUser.getParty;
                        String provider = contract.payload.getProvider.getParty;
                        return party.equals(user) || party.equals(provider);
                    })
                    .map(contract -> {
                        org.openapitools.model.AppInstallRequest appInstallRequest =
                                new org.openapitools.model.AppInstallRequest();
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
        }).exceptionally(ex -> {
            logger.error("Error listing AppInstallRequests", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        });
    }

    @Override
    public CompletableFuture<ResponseEntity<Void>> rejectAppInstallRequest(
            String contractId,
            String commandId,
            AppInstallRequestReject appInstallRequestReject
    ) {
        logger.info("Rejecting AppInstallRequest with contractId: {}", contractId);
        String providerParty = authenticatedPartyProvider.getPartyOrFail();
        return damlRepository.findAppInstallRequestById(contractId).thenCompose(contract -> {
            // Create choice instance
            quickstart_licensing.licensing.appinstall.AppInstallRequest.AppInstallRequest_Reject choice =
                    new quickstart_licensing.licensing.appinstall.AppInstallRequest.AppInstallRequest_Reject(
                            new quickstart_licensing.licensing.util.Metadata(appInstallRequestReject.getMeta().getData())
                    );

            return ledger.exerciseAndGetResult(providerParty, contract.contractId, choice, commandId)
                    .thenApply(result -> ResponseEntity.ok().<Void>build())
                    .exceptionally(ex -> {
                        logger.error("Error rejecting AppInstallRequest", ex);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    });
        }).exceptionally(ex -> {
            logger.error("Error fetching AppInstallRequest with contractId: {}", contractId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        });
    }
}
