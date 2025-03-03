// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.api.LicensesApi;
import com.digitalasset.quickstart.ledger.LedgerApi;
import com.digitalasset.quickstart.security.AuthenticatedPartyProvider;
import com.digitalasset.quickstart.repository.DamlRepository;
import com.digitalasset.transcode.java.ContractId;
import com.digitalasset.transcode.java.Party;
import daml_stdlib_da_time_types.da.time.types.RelTime;
import daml_prim_da_types.da.types.Tuple2;
import io.grpc.StatusRuntimeException;
import quickstart_licensing.licensing.license.License.License_Expire;
import quickstart_licensing.licensing.license.License.License_Renew;
import quickstart_licensing.licensing.license.LicenseRenewalRequest;
import quickstart_licensing.licensing.util.Metadata;
import org.openapitools.model.LicenseExpireRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import splice_wallet_payments.splice.wallet.payment.AppPaymentRequest;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class LicenseApiImpl implements LicensesApi {

    private final LedgerApi ledger;
    private final DamlRepository damlRepository;
    private final AuthenticatedPartyProvider authenticatedPartyProvider;
    private final Logger logger = LoggerFactory.getLogger(LicenseApiImpl.class);

    @Autowired
    public LicenseApiImpl(
            LedgerApi ledger,
            DamlRepository damlRepository,
            AuthenticatedPartyProvider authenticatedPartyProvider
    ) {
        this.ledger = ledger;
        this.damlRepository = damlRepository;
        this.authenticatedPartyProvider = authenticatedPartyProvider;
    }

    @Override
    public CompletableFuture<ResponseEntity<String>> expireLicense(
            String contractId,
            String commandId,
            LicenseExpireRequest licenseExpireRequest
    ) {
        logger.info("Expiring License with contractId: {}", contractId);
        String actingParty = authenticatedPartyProvider.getPartyOrFail();

        return damlRepository.findLicenseById(contractId).thenCompose(contract -> {
            Metadata meta = new Metadata(licenseExpireRequest.getMeta().getData());
            License_Expire choice = new License_Expire(new Party(actingParty), meta);

            return ledger.exerciseAndGetResult(actingParty, contract.contractId, choice, commandId)
                    .thenApply(result -> {
                        // If successful, return an OK response
                        return ResponseEntity.ok("License expired successfully");
                    })
                    .exceptionally(ex -> {
                        // Log the error
                        logger.error("Error expiring License", ex);

                        // Unwrap CompletionExceptions to get the real cause
                        Throwable cause = ex;
                        while (cause instanceof CompletionException && cause.getCause() != null) {
                            cause = cause.getCause();
                        }

                        if (cause instanceof StatusRuntimeException) {
                            if (cause.getMessage().contains("not expired yet")) {
                                return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body("License is not expired yet");
                            }
                            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(cause.getMessage());
                        } else {
                            // Otherwise return a generic error message
                            return ResponseEntity
                                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body("Internal error while expiring License");
                        }
                    });
        }).exceptionally(ex -> {
            // If we fail to fetch the License itself, also return 500
            logger.error("Error fetching License with contractId: {}", contractId, ex);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching License");
        });
    }


    @Override
    public CompletableFuture<ResponseEntity<List<org.openapitools.model.License>>> listLicenses() {
        logger.info("Listing Licenses");
        String party = authenticatedPartyProvider.getPartyOrFail();

        return damlRepository.findActiveLicenses().thenApply(contracts -> {
            List<org.openapitools.model.License> result = contracts.stream()
                    .filter(contract -> {
                        // Only show licenses where the requesting party is either the provider or user.
                        String user = contract.payload.getUser.getParty;
                        String provider = contract.payload.getProvider.getParty;
                        return user.equals(party) || provider.equals(party);
                    })
                    .map(contract -> {
                        org.openapitools.model.License l = new org.openapitools.model.License();
                        l.setContractId(contract.contractId.getContractId);
                        l.setDso(contract.payload.getDso.getParty);
                        l.setProvider(contract.payload.getProvider.getParty);
                        l.setUser(contract.payload.getUser.getParty);

                        // Map LicenseParams
                        org.openapitools.model.LicenseParams lp = new org.openapitools.model.LicenseParams();
                        org.openapitools.model.Metadata meta = new org.openapitools.model.Metadata();
                        meta.setData(contract.payload.getParams.getMeta.getValues);
                        lp.setMeta(meta);

                        l.setParams(lp);
                        l.setExpiresAt(OffsetDateTime.ofInstant(contract.payload.getExpiresAt, ZoneOffset.UTC));
                        l.setLicenseNum(contract.payload.getLicenseNum.intValue());

                        return l;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        }).exceptionally(ex -> {
            logger.error("Error listing Licenses", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        });
    }

    @Override
    public CompletableFuture<ResponseEntity<Void>> renewLicense(
            String contractId,
            String commandId,
            org.openapitools.model.LicenseRenewRequest licenseRenewRequest
    ) {
        logger.info("Renewing License with contractId: {}", contractId);
        String providerParty = authenticatedPartyProvider.getPartyOrFail();

        return damlRepository.findLicenseById(contractId).thenCompose(contract -> {
            Duration extDuration = Duration.parse(licenseRenewRequest.getLicenseExtensionDuration());
            long extensionMicros = extDuration.toNanos() / 1000;
            RelTime licenseExtensionDuration = new RelTime(extensionMicros);

            Duration payDuration = Duration.parse(licenseRenewRequest.getPaymentAcceptanceDuration());
            long payDurationMicros = payDuration.toNanos() / 1000;
            RelTime paymentAcceptanceDuration = new RelTime(payDurationMicros);

            String description = licenseRenewRequest.getDescription();

            License_Renew choice = new License_Renew(
                    licenseRenewRequest.getLicenseFeeCc(),
                    licenseExtensionDuration,
                    paymentAcceptanceDuration,
                    description
            );

            return ledger.exerciseAndGetResult(providerParty, contract.contractId, choice, commandId)
                    .thenApply((Tuple2<ContractId<LicenseRenewalRequest>, ContractId<AppPaymentRequest>> result) -> {
                        logger.info("Successfully renewed License {}. RenewalRequest ContractId: {} and PaymentRequest ContractId: {}",
                                contractId, result.get_1.getContractId, result.get_2.getContractId);
                        return ResponseEntity.ok().<Void>build();
                    }).exceptionally(ex -> {
                        logger.error("Error renewing License", ex);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    });
        }).exceptionally(ex -> {
            logger.error("Error fetching License with contractId: {}", contractId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        });
    }
}
