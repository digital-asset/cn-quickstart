// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.daml.ledger.api.v2.CommandsOuterClass;
import com.daml.ledger.api.v2.ValueOuterClass;
import com.digitalasset.quickstart.api.LicenseRenewalRequestsApi;
import com.digitalasset.quickstart.ledger.LedgerApi;
import com.digitalasset.quickstart.ledger.ScanProxy;
import com.digitalasset.quickstart.oauth.AuthenticatedPartyProvider;
import com.digitalasset.quickstart.repository.DamlRepository;
import com.digitalasset.transcode.java.ContractId;
import com.google.protobuf.ByteString;
import quickstart_licensing.licensing.license.License;
import quickstart_licensing.licensing.license.LicenseRenewalRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import splice_amulet.splice.amuletrules.AppTransferContext;
import splice_wallet_payments.splice.wallet.payment.AcceptedAppPayment;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class LicenseRenewalRequestsApiImpl implements LicenseRenewalRequestsApi {

    private final LedgerApi ledger;
    private final DamlRepository damlRepository;
    private final AuthenticatedPartyProvider authenticatedPartyProvider;
    private final ScanProxy scanProxyService;
    private final Logger logger = LoggerFactory.getLogger(LicenseRenewalRequestsApiImpl.class);

    @Autowired
    public LicenseRenewalRequestsApiImpl(
            LedgerApi ledger,
            DamlRepository damlRepository,
            AuthenticatedPartyProvider authenticatedPartyProvider,
            ScanProxy scanProxyService
    ) {
        this.ledger = ledger;
        this.damlRepository = damlRepository;
        this.authenticatedPartyProvider = authenticatedPartyProvider;
        this.scanProxyService = scanProxyService;
    }

    @Override
    public CompletableFuture<ResponseEntity<List<org.openapitools.model.LicenseRenewalRequest>>> listLicenseRenewalRequests() {
        logger.info("Listing LicenseRenewalRequests");
        String party = authenticatedPartyProvider.getPartyOrFail();

        return damlRepository.findActiveLicenseRenewalRequests()
                .thenApply(contracts -> {
                    List<org.openapitools.model.LicenseRenewalRequest> result = contracts.stream()
                            .filter(contract -> {
                                // Only show LicenseRenewalRequests where the requesting party is either the provider or user.
                                String user = contract.payload.getUser.getParty;
                                String provider = contract.payload.getProvider.getParty;
                                return user.equals(party) || provider.equals(party);
                            })
                            .map(contract -> {
                                org.openapitools.model.LicenseRenewalRequest r = new org.openapitools.model.LicenseRenewalRequest();
                                r.setContractId(contract.contractId.getContractId);
                                r.setProvider(contract.payload.getProvider.getParty);
                                r.setUser(contract.payload.getUser.getParty);
                                r.setDso(contract.payload.getDso.getParty);
                                r.setLicenseNum(contract.payload.getLicenseNum.intValue());
                                r.setLicenseFeeCc(contract.payload.getLicenseFeeCc);

                                String relTimeReadable = (contract.payload.getLicenseExtensionDuration.getMicroseconds
                                        / 1000 / 1000 / 60 / 60 / 24) + " days";
                                r.setLicenseExtensionDuration(relTimeReadable);

                                r.setReference(contract.payload.getReference.getContractId);
                                return r;
                            })
                            .collect(Collectors.toList());

                    return ResponseEntity.ok(result);
                })
                .exceptionally(ex -> {
                    logger.error("Error listing LicenseRenewalRequests", ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }

    @Override
    public CompletableFuture<ResponseEntity<Void>> completeLicenseRenewal(
            String contractId,
            String commandId
    ) {
        logger.info("Completing License RenewalRequest with contractId: {}", contractId);
        String actingParty = authenticatedPartyProvider.getPartyOrFail();

        // 1. Fetch the LicenseRenewalRequest contract from DamlRepository
        return damlRepository.findLicenseRenewalRequestById(contractId)
                .thenCompose(lrrContract -> {
                    String user = lrrContract.payload.getUser.getParty;
                    String provider = lrrContract.payload.getProvider.getParty;
                    String dso = lrrContract.payload.getDso.getParty;
                    Long licenseNum = lrrContract.payload.getLicenseNum;
                    String referenceCid = lrrContract.payload.getReference.getContractId;

                    // Find the AcceptedAppPayment contract that matches the reference/user/provider
                    return damlRepository.findSingleActiveAcceptedAppPayment(referenceCid, user, provider)
                            .thenCompose(maybeAcceptedPayment -> {
                                if (maybeAcceptedPayment.isEmpty()) {
                                    logger.error("No AcceptedAppPayment found for reference: {}, user: {}, provider: {}",
                                            referenceCid, user, provider);
                                    return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
                                }

                                ContractId<AcceptedAppPayment> acceptedPaymentCid = maybeAcceptedPayment.get().contractId;
                                Long miningRound = maybeAcceptedPayment.get().payload.getRound.getNumber;

                                // Find the active License that matches user/provider/dso/licenseNum
                                return damlRepository.findSingleActiveLicense(user, provider, licenseNum, dso)
                                        .thenCompose(maybeLicense -> {
                                            if (maybeLicense.isEmpty()) {
                                                logger.error("No matching License contract found for user: {}, provider: {}, licenseNum: {}, dso: {}",
                                                        user, provider, licenseNum, dso);
                                                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
                                            }

                                            ContractId<License> licenseCid = maybeLicense.get().contractId;

                                            // Fetch the necessary disclosed contracts for the choice
                                            CompletableFuture<CommandsOuterClass.DisclosedContract> amuletRulesFut =
                                                    fetchAmuletRulesDisclosedContract();
                                            CompletableFuture<CommandsOuterClass.DisclosedContract> openMiningRoundFut =
                                                    fetchOpenMiningRoundDisclosedContract(miningRound);

                                            return CompletableFuture.allOf(amuletRulesFut, openMiningRoundFut)
                                                    .thenCompose(v -> {
                                                        CommandsOuterClass.DisclosedContract amuletRulesDc = amuletRulesFut.join();
                                                        CommandsOuterClass.DisclosedContract openMiningRoundDc = openMiningRoundFut.join();

                                                        AppTransferContext transferContext = new AppTransferContext(
                                                                new ContractId<>(amuletRulesDc.getContractId()),
                                                                new ContractId<>(openMiningRoundDc.getContractId()),
                                                                Optional.empty()
                                                        );

                                                        LicenseRenewalRequest.LicenseRenewalRequest_CompleteRenewal choice =
                                                                new LicenseRenewalRequest.LicenseRenewalRequest_CompleteRenewal(
                                                                        acceptedPaymentCid,
                                                                        licenseCid,
                                                                        transferContext
                                                                );

                                                        // 5. Exercise the choice on the ledger
                                                        return ledger.exerciseAndGetResult(
                                                                actingParty,
                                                                lrrContract.contractId,
                                                                choice,
                                                                commandId,
                                                                List.of(amuletRulesDc, openMiningRoundDc)
                                                        ).thenApply(newLicenseCid -> {
                                                            logger.info("License renewed successfully. New License contractId: {}",
                                                                    newLicenseCid.getContractId);
                                                            return ResponseEntity.ok().<Void>build();
                                                        }).exceptionally(ex2 -> {
                                                            logger.error("Error completing License Renewal", ex2);
                                                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                                                        });
                                                    });
                                        });
                            });
                })
                .exceptionally(ex -> {
                    logger.error("Error fetching LicenseRenewalRequest with contractId: {}", contractId, ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }

    private CompletableFuture<CommandsOuterClass.DisclosedContract> fetchAmuletRulesDisclosedContract() {
        return scanProxyService.getAmuletRules().thenApply(resp -> {
            var contract = resp.getAmuletRules().getContract();
            return buildDisclosedContractFromApi(
                    contract.getTemplateId(),
                    contract.getContractId(),
                    contract.getCreatedEventBlob());
        });
    }

    private CompletableFuture<CommandsOuterClass.DisclosedContract> fetchOpenMiningRoundDisclosedContract(Long roundNumber) {
        return scanProxyService.getOpenAndIssuingMiningRounds().thenApply(resp -> {
            if (resp.getOpenMiningRounds().isEmpty()) {
                throw new RuntimeException("No open mining rounds found");
            }
            var contract = resp.getOpenMiningRounds().stream()
                    .filter(round -> Long.valueOf(round.getContract().getPayload().getRound().getNumber()).equals(roundNumber))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No open mining round found with number: " + roundNumber))
                    .getContract();
            return buildDisclosedContractFromApi(
                    contract.getTemplateId(),
                    contract.getContractId(),
                    contract.getCreatedEventBlob());
        });
    }

    private CommandsOuterClass.DisclosedContract buildDisclosedContractFromApi(
            String templateIdStr, String contractId, String createdEventBlobBase64
    ) {
        ValueOuterClass.Identifier templateId = parseTemplateIdentifier(templateIdStr);
        byte[] blob = Base64.getDecoder().decode(createdEventBlobBase64);
        return CommandsOuterClass.DisclosedContract.newBuilder()
                .setTemplateId(templateId)
                .setContractId(contractId)
                .setCreatedEventBlob(ByteString.copyFrom(blob))
                .build();
    }

    private static ValueOuterClass.Identifier parseTemplateIdentifier(String templateIdStr) {
        String[] parts = templateIdStr.split(":");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid templateId format: " + templateIdStr);
        }

        String packageId = parts[0];
        String moduleName = parts[1];
        StringBuilder entityNameBuilder = new StringBuilder();
        for (int i = 2; i < parts.length; i++) {
            if (i > 2) entityNameBuilder.append(":");
            entityNameBuilder.append(parts[i]);
        }
        String entityName = entityNameBuilder.toString();

        return ValueOuterClass.Identifier.newBuilder()
                .setPackageId(packageId)
                .setModuleName(moduleName)
                .setEntityName(entityName)
                .build();
    }
}
