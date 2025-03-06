// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.daml.ledger.api.v2.CommandsOuterClass;
import com.daml.ledger.api.v2.ValueOuterClass;
import com.digitalasset.quickstart.api.LicenseRenewalRequestsApi;
import com.digitalasset.quickstart.ledger.LedgerApi;
import com.digitalasset.quickstart.ledger.ScanProxy;
import com.digitalasset.quickstart.security.AuthenticatedPartyProvider;
import com.digitalasset.quickstart.repository.DamlRepository;
import com.digitalasset.quickstart.utility.LoggingSpanHelper;
import com.digitalasset.transcode.java.ContractId;
import com.google.protobuf.ByteString;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.openapitools.model.LicenseRenewalRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import quickstart_licensing.licensing.license.License;
import quickstart_licensing.licensing.license.LicenseRenewalRequest.LicenseRenewalRequest_CompleteRenewal;
import splice_amulet.splice.amuletrules.AppTransferContext;
import splice_wallet_payments.splice.wallet.payment.AcceptedAppPayment;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.digitalasset.quickstart.utility.ContextAwareCompletableFutures.completeWithin;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class LicenseRenewalRequestsApiImpl implements LicenseRenewalRequestsApi {

    private final LedgerApi ledger;
    private final DamlRepository damlRepository;
    private final AuthenticatedPartyProvider authenticatedPartyProvider;
    private final ScanProxy scanProxyService;
    private final Logger logger = LoggerFactory.getLogger(LicenseRenewalRequestsApiImpl.class);

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
    @WithSpan
    public CompletableFuture<ResponseEntity<List<LicenseRenewalRequest>>> listLicenseRenewalRequests() {
        Span methodSpan = Span.current();
        Context parentContext = Context.current();

        methodSpan.addEvent("listLicenseRenewalRequests: Starting retrieval of license renewal requests");
        logger.atInfo().log("listLicenseRenewalRequests: Starting retrieval of license renewal requests");

        return authenticatedPartyService.getPartyOrFail()
                .thenCompose(party ->
                        damlRepository.findActiveLicenseRenewalRequests()
                                .thenApply(contracts -> {
                                    List<LicenseRenewalRequest> result = contracts.stream()
                                            .filter(contract -> {
                                                String user = contract.payload.getUser.getParty;
                                                String provider = contract.payload.getProvider.getParty;
                                                return user.equals(party) || provider.equals(party);
                                            })
                                            .map(contract -> {
                                                LicenseRenewalRequest r = new LicenseRenewalRequest();
                                                r.setContractId(contract.contractId.getContractId);
                                                r.setProvider(contract.payload.getProvider.getParty);
                                                r.setUser(contract.payload.getUser.getParty);
                                                r.setDso(contract.payload.getDso.getParty);
                                                r.setLicenseNum(contract.payload.getLicenseNum.intValue());
                                                r.setLicenseFeeCc(contract.payload.getLicenseFeeCc);
                                                String relTimeReadable =
                                                        (contract.payload.getLicenseExtensionDuration.getMicroseconds
                                                                / 1000 / 1000 / 60 / 60 / 24) + " days";
                                                r.setLicenseExtensionDuration(relTimeReadable);
                                                r.setReference(contract.payload.getReference.getContractId);
                                                return r;
                                            })
                                            .collect(Collectors.toList());
                                    return ResponseEntity.ok(result);
                                })
                )
                .whenComplete(
                        completeWithin(parentContext, (res, ex) -> {
                            if (ex == null) {
                                if (res != null && res.getBody() != null) {
                                    Map<String, Object> attrs = Map.of(
                                            "retrievedRequestsCount", res.getBody().size()
                                    );
                                    LoggingSpanHelper.addEventWithAttributes(methodSpan,
                                            "listLicenseRenewalRequests: Successfully retrieved requests",
                                            attrs
                                    );
                                    LoggingSpanHelper.logDebug(logger,
                                            "listLicenseRenewalRequests: Successfully retrieved requests",
                                            attrs
                                    );
                                } else {
                                    Map<String, Object> attrs = Map.of("retrievedRequestsCount", 0);
                                    LoggingSpanHelper.addEventWithAttributes(methodSpan,
                                            "listLicenseRenewalRequests: Successfully retrieved requests",
                                            attrs
                                    );
                                    LoggingSpanHelper.logDebug(logger,
                                            "listLicenseRenewalRequests: Successfully retrieved requests",
                                            attrs
                                    );
                                }
                            } else {
                                LoggingSpanHelper.logError(
                                        logger,
                                        "listLicenseRenewalRequests: Failed to retrieve requests",
                                        ex
                                );
                                LoggingSpanHelper.recordException(methodSpan, ex);
                            }
                        })
                );
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<Void>> completeLicenseRenewal(
            @SpanAttribute("licenseRenewal.contractId") String contractId,
            @SpanAttribute("licenseRenewal.commandId") String commandId
    ) {
        Span methodSpan = Span.current();
        Context parentContext = Context.current();

        Map<String, Object> initialAttrs = Map.of(
                "contractId", contractId,
                "commandId", commandId
        );
        LoggingSpanHelper.addEventWithAttributes(methodSpan,
                "completeLicenseRenewal: Starting",
                initialAttrs
        );
        LoggingSpanHelper.logInfo(logger,
                "completeLicenseRenewal: received request",
                initialAttrs
        );

        return authenticatedPartyService.getPartyOrFail()
                .<ResponseEntity<Void>>thenCompose(actingParty ->
                        damlRepository.findLicenseRenewalRequestById(contractId)
                                .thenCompose(lrrContract -> {
                                    LoggingSpanHelper.addEventWithAttributes(methodSpan,
                                            "completeLicenseRenewal: Fetched LicenseRenewalRequest contract",
                                            initialAttrs
                                    );
                                    LoggingSpanHelper.logDebug(logger,
                                            "completeLicenseRenewal: Fetched LicenseRenewalRequest contract",
                                            initialAttrs
                                    );

                                    String user = lrrContract.payload.getUser.getParty;
                                    String provider = lrrContract.payload.getProvider.getParty;
                                    String dso = lrrContract.payload.getDso.getParty;
                                    Long licenseNum = lrrContract.payload.getLicenseNum;
                                    String referenceCid = lrrContract.payload.getReference.getContractId;

                                    return damlRepository.findSingleActiveAcceptedAppPayment(referenceCid, user, provider)
                                            .thenCompose(maybeAcceptedPayment -> {
                                                if (maybeAcceptedPayment.isEmpty()) {
                                                    Map<String, Object> noPaymentAttrs = Map.of(
                                                            "commandId", commandId,
                                                            "referenceCid", referenceCid,
                                                            "user", user,
                                                            "provider", provider
                                                    );
                                                    LoggingSpanHelper.logError(
                                                            logger,
                                                            "completeLicenseRenewal: No AcceptedAppPayment found",
                                                            noPaymentAttrs,
                                                            null
                                                    );
                                                    LoggingSpanHelper.addEventWithAttributes(methodSpan,
                                                            "completeLicenseRenewal: No AcceptedAppPayment found",
                                                            noPaymentAttrs
                                                    );
                                                    return CompletableFuture.completedFuture(
                                                            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                                                    );
                                                }

                                                ContractId<AcceptedAppPayment> acceptedPaymentCid =
                                                        maybeAcceptedPayment.get().contractId;
                                                Long miningRound =
                                                        maybeAcceptedPayment.get().payload.getRound.getNumber;

                                                return damlRepository.findSingleActiveLicense(user, provider, licenseNum, dso)
                                                        .thenCompose(maybeLicense -> {
                                                            if (maybeLicense.isEmpty()) {
                                                                Map<String, Object> noLicenseAttrs = Map.of(
                                                                        "user", user,
                                                                        "provider", provider,
                                                                        "licenseNum", licenseNum,
                                                                        "dso", dso
                                                                );
                                                                LoggingSpanHelper.logError(
                                                                        logger,
                                                                        "completeLicenseRenewal: No matching License found",
                                                                        noLicenseAttrs,
                                                                        null
                                                                );
                                                                LoggingSpanHelper.addEventWithAttributes(methodSpan,
                                                                        "completeLicenseRenewal: No matching License found",
                                                                        noLicenseAttrs
                                                                );
                                                                return CompletableFuture.completedFuture(
                                                                        ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                                                                );
                                                            }

                                                            ContractId<License> licenseCid =
                                                                    maybeLicense.get().contractId;

                                                            CompletableFuture<CommandsOuterClass.DisclosedContract> amuletRulesFut =
                                                                    fetchAmuletRulesDisclosedContract();
                                                            CompletableFuture<CommandsOuterClass.DisclosedContract> openMiningRoundFut =
                                                                    fetchOpenMiningRoundDisclosedContract(miningRound);

                                                            return CompletableFuture.allOf(amuletRulesFut, openMiningRoundFut)
                                                                    .thenCompose(unused -> {
                                                                        CommandsOuterClass.DisclosedContract amuletRulesDc =
                                                                                amuletRulesFut.join();
                                                                        CommandsOuterClass.DisclosedContract openMiningRoundDc =
                                                                                openMiningRoundFut.join();

                                                                        AppTransferContext transferContext = new AppTransferContext(
                                                                                new ContractId<>(amuletRulesDc.getContractId()),
                                                                                new ContractId<>(openMiningRoundDc.getContractId()),
                                                                                Optional.empty()
                                                                        );

                                                                        LicenseRenewalRequest_CompleteRenewal choice =
                                                                                new LicenseRenewalRequest_CompleteRenewal(
                                                                                        acceptedPaymentCid,
                                                                                        licenseCid,
                                                                                        transferContext
                                                                                );

                                                                        Map<String, Object> choiceAttrs = Map.of(
                                                                                "choiceName",
                                                                                "LicenseRenewalRequest_CompleteRenewal"
                                                                        );
                                                                        LoggingSpanHelper.addEventWithAttributes(methodSpan,
                                                                                "completeLicenseRenewal: Exercising choice",
                                                                                choiceAttrs
                                                                        );
                                                                        LoggingSpanHelper.logDebug(logger,
                                                                                "completeLicenseRenewal: Exercising choice",
                                                                                choiceAttrs
                                                                        );

                                                                        return ledger.exerciseAndGetResult(
                                                                                actingParty,
                                                                                lrrContract.contractId,
                                                                                choice,
                                                                                commandId,
                                                                                List.of(amuletRulesDc, openMiningRoundDc)
                                                                        ).thenApply(newLicenseCid -> {
                                                                            Map<String, Object> successAttrs =
                                                                                    Map.of(
                                                                                            "contractId", contractId,
                                                                                            "commandId", commandId,
                                                                                            "newLicenseContractId",
                                                                                            newLicenseCid.getContractId
                                                                                    );
                                                                            LoggingSpanHelper.addEventWithAttributes(methodSpan,
                                                                                    "completeLicenseRenewal: Successfully renewed license",
                                                                                    successAttrs
                                                                            );
                                                                            LoggingSpanHelper.logInfo(logger,
                                                                                    "completeLicenseRenewal: Successfully renewed license",
                                                                                    successAttrs
                                                                            );
                                                                            return ResponseEntity.ok().<Void>build();
                                                                        });
                                                                    });
                                                        });
                                            });
                                })
                )
                .whenComplete(
                        completeWithin(parentContext, (res, ex) -> {
                            if (ex == null) {
                                Map<String, Object> successAttrs = Map.of("contractId", contractId);
                                LoggingSpanHelper.addEventWithAttributes(methodSpan,
                                        "completeLicenseRenewal: Success",
                                        successAttrs
                                );
                                LoggingSpanHelper.logDebug(logger,
                                        "completeLicenseRenewal: Success",
                                        successAttrs
                                );
                            } else {
                                Map<String, Object> errorAttrs = Map.of("contractId", contractId);
                                LoggingSpanHelper.logError(logger,
                                        "completeLicenseRenewal: Failed",
                                        errorAttrs,
                                        ex
                                );
                                LoggingSpanHelper.recordException(methodSpan, ex);
                            }
                        })
                );
    }

    private CompletableFuture<CommandsOuterClass.DisclosedContract> fetchAmuletRulesDisclosedContract() {
        return scanProxyService.getAmuletRules().thenApply(resp -> {
            var contract = resp.getAmuletRules().getContract();
            return buildDisclosedContractFromApi(
                    contract.getTemplateId(),
                    contract.getContractId(),
                    contract.getCreatedEventBlob()
            );
        });
    }

    private CompletableFuture<CommandsOuterClass.DisclosedContract> fetchOpenMiningRoundDisclosedContract(Long roundNumber) {
        return scanProxyService.getOpenAndIssuingMiningRounds().thenCompose(resp -> {
            if (resp.getOpenMiningRounds().isEmpty()) {
                return CompletableFuture.failedFuture(new RuntimeException("No open mining rounds found"));
            }
            var maybeContract = resp.getOpenMiningRounds().stream()
                    .filter(round -> Long.valueOf(round.getContract().getPayload().getRound().getNumber())
                            .equals(roundNumber))
                    .findFirst();
            if (maybeContract.isEmpty()) {
                return CompletableFuture.failedFuture(
                        new IllegalStateException(
                                "No open mining round found with number: " + roundNumber
                        )
                );
            }
            var contract = maybeContract.get().getContract();
            return CompletableFuture.completedFuture(
                    buildDisclosedContractFromApi(
                            contract.getTemplateId(),
                            contract.getContractId(),
                            contract.getCreatedEventBlob()
                    )
            );
        });
    }

    private CommandsOuterClass.DisclosedContract buildDisclosedContractFromApi(
            String templateIdStr,
            String contractId,
            String createdEventBlobBase64
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
            if (i > 2) {
                entityNameBuilder.append(":");
            }
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
