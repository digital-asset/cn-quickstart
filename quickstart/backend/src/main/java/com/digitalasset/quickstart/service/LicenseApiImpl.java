// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.daml.ledger.api.v2.CommandsOuterClass;
import com.daml.ledger.api.v2.ValueOuterClass;
import com.digitalasset.quickstart.api.LicensesApi;
import com.digitalasset.quickstart.ledger.LedgerApi;
import com.digitalasset.quickstart.ledger.ScanProxy;
import com.digitalasset.quickstart.pqs.Contract;
import com.digitalasset.quickstart.repository.DamlRepository;
import com.digitalasset.quickstart.repository.DamlRepository.LicenseRenewalRequestData;
import com.digitalasset.quickstart.security.AuthenticatedPartyProvider;
import com.digitalasset.quickstart.utility.LoggingSpanHelper;
import com.digitalasset.transcode.java.ContractId;
import com.digitalasset.transcode.java.Party;
import com.google.protobuf.ByteString;
import daml_stdlib_da_time_types.da.time.types.RelTime;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.openapitools.model.LicenseExpireRequest;
import org.openapitools.model.LicenseRenewRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import quickstart_licensing.licensing.license.License;
import quickstart_licensing.licensing.license.License.License_Expire;
import quickstart_licensing.licensing.license.License.License_Renew;
import quickstart_licensing.licensing.license.LicenseRenewalRequest;
import quickstart_licensing.licensing.license.LicenseRenewalRequest.LicenseRenewalRequest_CompleteRenewal;
import splice_api_token_allocation_v1.splice.api.token.allocationv1.Allocation;
import splice_api_token_metadata_v1.splice.api.token.metadatav1.ChoiceContext;
import splice_api_token_metadata_v1.splice.api.token.metadatav1.ExtraArgs;
import splice_api_token_metadata_v1.splice.api.token.metadatav1.Metadata;
import splice_wallet_payments.splice.wallet.payment.AcceptedAppPayment;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.digitalasset.quickstart.utility.ContextAwareCompletableFutures.completeWithin;
import static com.digitalasset.quickstart.utility.ContextAwareCompletableFutures.supplyWithin;

/**
 * License management service for handling contract-based operations on Licenses.
 */
@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class LicenseApiImpl implements LicensesApi {

    private static final Logger logger = LoggerFactory.getLogger(LicenseApiImpl.class);

    private final LedgerApi ledger;
    private final DamlRepository damlRepository;
    private final AuthenticatedPartyProvider authenticatedPartyProvider;
    private final ScanProxy scanProxyService;

    public LicenseApiImpl(
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

    /**
     * Lists active License contracts visible to the authenticated party.
     */
    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<List<org.openapitools.model.License>>> listLicenses() {
        Span methodSpan = Span.current();
        Context parentContext = Context.current();
        Map<String, Object> startAttributes = Map.of("templateId", License.TEMPLATE_ID.qualifiedName());

        LoggingSpanHelper.addEventWithAttributes(methodSpan, "listLicenses: start", startAttributes);
        LoggingSpanHelper.setSpanAttributes(methodSpan, startAttributes);

        return CompletableFuture.completedFuture(authenticatedPartyProvider.getPartyOrFail())
                .thenCompose(party ->
                        CompletableFuture
                                .supplyAsync(
                                        supplyWithin(parentContext, () -> fetchLicensesAndRenewals(party, methodSpan))
                                )
                                .thenCompose(cf -> cf)
                                .thenApply(ResponseEntity::ok)
                )
                .whenComplete(
                        completeWithin(parentContext, (res, ex) -> {
                            if (ex == null) {
                                int count = (res.getBody() != null) ? res.getBody().size() : 0;
                                LoggingSpanHelper.logDebug(logger, "listLicenses: success", Map.of("count", count));
                            } else {
                                LoggingSpanHelper.logError(logger, "listLicenses: failed", startAttributes, ex);
                                LoggingSpanHelper.recordException(methodSpan, ex);
                            }
                        })
                );
    }

    private CompletableFuture<List<org.openapitools.model.License>> fetchLicensesAndRenewals(
            String party,
            Span span
    ) {
        LoggingSpanHelper.addEventWithAttributes(span, "Fetching licenses and renewal requests", Map.of("party", party));

        return damlRepository.findActiveLicensesByParty(party)
                .thenCompose(licenseContracts ->
                        damlRepository.findActiveLicenseRenewalRequestsByParty(party)
                                .thenApply(renewalRecords ->
                                        buildLicenseList(licenseContracts, renewalRecords)
                                )
                );
    }

    private static record RenewalKey(String dso, String provider, String user, int licenseNum) {}

    /**
     * Builds a list of License objects by associating each license contract with its matching renewals.
     */
    private List<org.openapitools.model.License> buildLicenseList(
            List<Contract<License>> licenseContracts,
            List<LicenseRenewalRequestData> renewalContracts
    ) {
        Map<RenewalKey, List<LicenseRenewalRequestData>> groupedRenewals =
                renewalContracts.stream().collect(
                        Collectors.groupingBy(rcData ->
                                new RenewalKey(
                                        rcData.contract().payload.getDso.getParty,
                                        rcData.contract().payload.getProvider.getParty,
                                        rcData.contract().payload.getUser.getParty,
                                        rcData.contract().payload.getLicenseNum.intValue()
                                )
                        )
                );

        return licenseContracts.stream()
                .map(contract -> {
                    org.openapitools.model.License apiLicense = new org.openapitools.model.License();
                    apiLicense.setContractId(contract.contractId.getContractId);
                    apiLicense.setDso(contract.payload.getDso.getParty);
                    apiLicense.setProvider(contract.payload.getProvider.getParty);
                    apiLicense.setUser(contract.payload.getUser.getParty);

                    org.openapitools.model.LicenseParams lp = new org.openapitools.model.LicenseParams();
                    org.openapitools.model.Metadata meta = new org.openapitools.model.Metadata();
                    meta.setData(contract.payload.getParams.getMeta.getValues);
                    lp.setMeta(meta);
                    apiLicense.setParams(lp);

                    apiLicense.setExpiresAt(
                            OffsetDateTime.ofInstant(contract.payload.getExpiresAt, ZoneOffset.UTC)
                    );
                    apiLicense.setLicenseNum(contract.payload.getLicenseNum.intValue());

                    RenewalKey key = new RenewalKey(
                            contract.payload.getDso.getParty,
                            contract.payload.getProvider.getParty,
                            contract.payload.getUser.getParty,
                            contract.payload.getLicenseNum.intValue()
                    );

                    List<LicenseRenewalRequestData> matchedRenewals =
                            groupedRenewals.getOrDefault(key, Collections.emptyList());

                    List<org.openapitools.model.LicenseRenewalRequest> apiRenewals =
                            matchedRenewals.stream()
                                    .map(rcData -> {
                                        Contract<LicenseRenewalRequest> rc = rcData.contract();
                                        org.openapitools.model.LicenseRenewalRequest r =
                                                new org.openapitools.model.LicenseRenewalRequest();
                                        r.setContractId(rc.contractId.getContractId);
                                        r.setProvider(rc.payload.getProvider.getParty);
                                        r.setUser(rc.payload.getUser.getParty);
                                        r.setDso(rc.payload.getDso.getParty);
                                        r.setLicenseNum(rc.payload.getLicenseNum.intValue());
                                        r.setLicenseFeeCc(rc.payload.getLicenseFeeAmount);
                                        r.setIsPaid(rcData.isPaid());

                                        long micros = rc.payload.getLicenseExtensionDuration.getMicroseconds;
                                        String approximateDays = (micros / 1_000_000 / 3600 / 24) + " days";
                                        r.setLicenseExtensionDuration(approximateDays);
                                        return r;
                                    })
                                    .collect(Collectors.toList());

                    apiLicense.setRenewalRequests(apiRenewals);
                    return apiLicense;
                })
                .collect(Collectors.toList());
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<Void>> renewLicense(
            @SpanAttribute("license.contractId") String contractId,
            @SpanAttribute("license.commandId") String commandId,
            LicenseRenewRequest licenseRenewRequest
    ) {
        Span methodSpan = Span.current();
        Context parentContext = Context.current();

        Map<String, Object> attributes = Map.of(
                "license.contractId", contractId,
                "license.commandId", commandId,
                "templateId", License.TEMPLATE_ID.qualifiedName(),
                "choiceName", "License_Renew"
        );
        LoggingSpanHelper.addEventWithAttributes(methodSpan, "renewLicense: start", attributes);
        LoggingSpanHelper.setSpanAttributes(methodSpan, attributes);

        return CompletableFuture.completedFuture(authenticatedPartyProvider.getPartyOrFail())
                .thenCompose(providerParty ->
                        CompletableFuture.supplyAsync(
                                supplyWithin(parentContext, () ->
                                        exerciseLicenseRenewChoice(providerParty, contractId, commandId, licenseRenewRequest, attributes, methodSpan)
                                )
                        ).thenCompose(cf -> cf)
                )
                .whenComplete(
                        completeWithin(parentContext, (res, ex) -> {
                            if (ex == null) {
                                LoggingSpanHelper.logDebug(logger, "renewLicense: success", attributes);
                            } else {
                                LoggingSpanHelper.logError(logger, "renewLicense: failed", attributes, ex);
                                LoggingSpanHelper.recordException(methodSpan, ex);
                            }
                        })
                );
    }

    private CompletableFuture<ResponseEntity<Void>> exerciseLicenseRenewChoice(
            String providerParty,
            String contractId,
            String commandId,
            LicenseRenewRequest licenseRenewRequest,
            Map<String, Object> attributes,
            Span span
    ) {
        LoggingSpanHelper.addEventWithAttributes(span, "Exercising License_Renew", attributes);

        return damlRepository.findLicenseById(contractId)
                .thenCompose(contract -> {
                    Duration extDuration = Duration.parse(licenseRenewRequest.getLicenseExtensionDuration());
                    long extensionMicros = extDuration.toNanos() / 1_000;
                    RelTime licenseExtensionDuration = new RelTime(extensionMicros);

                    Duration payDuration = Duration.parse(licenseRenewRequest.getPaymentAcceptanceDuration());
                    long payDurationMicros = payDuration.toNanos() / 1_000;
                    RelTime paymentAcceptanceDuration = new RelTime(payDurationMicros);

                    License_Renew choice = new License_Renew(
                            licenseRenewRequest.getLicenseFeeCc(),
                            licenseExtensionDuration,
                            paymentAcceptanceDuration,
                            licenseRenewRequest.getDescription()
                    );

                    return ledger.exerciseAndGetResult(
                            providerParty,
                            contract.contractId,
                            choice,
                            commandId
                    ).thenApply(result -> {
                        LoggingSpanHelper.logInfo(logger, "License renewal request succeeded", attributes);
                        return ResponseEntity.ok().<Void>build();
                    });
                });
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<Void>> completeLicenseRenewal(
            @SpanAttribute("licenseRenewal.contractId") String renewalRequestId,
            @SpanAttribute("licenseRenewal.commandId") String commandId
    ) {
        Span methodSpan = Span.current();
        Context parentContext = Context.current();

        Map<String, Object> initialAttrs = Map.of(
                "renewalRequestId", renewalRequestId,
                "commandId", commandId
        );
        LoggingSpanHelper.addEventWithAttributes(methodSpan, "completeLicenseRenewal: start", initialAttrs);
        LoggingSpanHelper.logInfo(logger, "completeLicenseRenewal: received request", initialAttrs);

        return CompletableFuture.completedFuture(authenticatedPartyProvider.getPartyOrFail())
                .thenCompose(actingParty ->
                        damlRepository.findLicenseRenewalRequestById(renewalRequestId)
                                .thenCompose(lrrContract -> {
                                    if (lrrContract == null) {
                                        LoggingSpanHelper.logError(logger, "No LicenseRenewalRequest found", initialAttrs, null);
                                        return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
                                    }
                                    return processLicenseRenewal(actingParty, commandId, lrrContract, initialAttrs, methodSpan);
                                })
                )
                .whenComplete(
                        completeWithin(parentContext, (res, ex) -> {
                            if (ex == null) {
                                LoggingSpanHelper.logDebug(logger, "completeLicenseRenewal: success", initialAttrs);
                            } else {
                                LoggingSpanHelper.logError(logger, "completeLicenseRenewal: failed", initialAttrs, ex);
                                LoggingSpanHelper.recordException(methodSpan, ex);
                            }
                        })
                );
    }

    private CompletableFuture<ResponseEntity<Void>> processLicenseRenewal(
            String actingParty,
            String commandId,
            Contract<LicenseRenewalRequest> lrrContract,
            Map<String, Object> initialAttrs,
            Span span
    ) {
        // The renewal is finalized by referencing a token-standard Allocation contract that implements the actual fee
        // transfer contract that implements the actual fee transfer. Once the Allocation is executed,
        // the choice LicenseRenewalRequest_CompleteRenewal creates the new license contract.
        return damlRepository.findActiveAllocationForRenewalRequest(lrrContract.contractId.getContractId)
                .thenCompose(allocationOpt -> {
                    if (allocationOpt.isEmpty()) {
                        LoggingSpanHelper.logError(logger, "No corresponding Allocation for this renewal request", initialAttrs, null);
                        return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
                    }

                    Contract<Allocation> allocationContract = allocationOpt.get();

                    ExtraArgs extraArgs = new ExtraArgs(
                            new ChoiceContext(Map.of()),
                            new Metadata(Map.of())
                    );

                    LicenseRenewalRequest_CompleteRenewal choice =
                            new LicenseRenewalRequest_CompleteRenewal(
                                    allocationContract.contractId,
                                    extraArgs
                            );

                    return ledger.exerciseAndGetResult(
                            actingParty,
                            lrrContract.contractId,
                            choice,
                            commandId
                    ).thenApply(newLicenseCid -> {
                        Map<String, Object> successAttrs = new HashMap<>(initialAttrs);
                        successAttrs.put("newLicenseContractId", newLicenseCid.getContractId);
                        LoggingSpanHelper.logInfo(logger, "completeLicenseRenewal: License renewed", successAttrs);
                        return ResponseEntity.ok().<Void>build();
                    });
                });
    }

    /**
     * Expires a License contract by exercising the 'License_Expire' choice.
     */
    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<String>> expireLicense(
            @SpanAttribute("contractId") String contractId,
            @SpanAttribute("commandId") String commandId,
            LicenseExpireRequest licenseExpireRequest
    ) {
        Span methodSpan = Span.current();
        Context parentContext = Context.current();

        Map<String, Object> attributes = Map.of(
                "contractId", contractId,
                "commandId", commandId,
                "templateId", License.TEMPLATE_ID.qualifiedName(),
                "choiceName", "License_Expire"
        );
        LoggingSpanHelper.addEventWithAttributes(methodSpan, "expireLicense: start", attributes);
        LoggingSpanHelper.setSpanAttributes(methodSpan, attributes);
        LoggingSpanHelper.logInfo(logger, "expireLicense: received request", attributes);

        return CompletableFuture.completedFuture(authenticatedPartyProvider.getPartyOrFail())
                .thenCompose(actingParty ->
                        CompletableFuture.supplyAsync(
                                supplyWithin(parentContext, () ->
                                        exerciseLicenseExpireChoice(actingParty, contractId, commandId, licenseExpireRequest, attributes, methodSpan)
                                )
                        ).thenCompose(cf -> cf)
                )
                .whenComplete(
                        completeWithin(parentContext, (res, ex) -> {
                            if (ex == null) {
                                LoggingSpanHelper.logDebug(logger, "expireLicense: success", attributes);
                            } else {
                                LoggingSpanHelper.logError(logger, "expireLicense: failed", attributes, ex);
                                LoggingSpanHelper.recordException(methodSpan, ex);
                            }
                        })
                );
    }

    private CompletableFuture<ResponseEntity<String>> exerciseLicenseExpireChoice(
            String actingParty,
            String contractId,
            String commandId,
            LicenseExpireRequest licenseExpireRequest,
            Map<String, Object> attributes,
            Span span
    ) {
        LoggingSpanHelper.addEventWithAttributes(span, "Exercising License_Expire", attributes);

        return damlRepository.findLicenseById(contractId)
                .thenCompose(contract -> {
                    Metadata meta = new Metadata(licenseExpireRequest.getMeta().getData());
                    License_Expire choice = new License_Expire(new Party(actingParty), meta);

                    return ledger.exerciseAndGetResult(actingParty, contract.contractId, choice, commandId)
                            .thenApply(result -> {
                                LoggingSpanHelper.logInfo(logger, "License expired successfully", attributes);
                                return ResponseEntity.ok("License expired successfully");
                            });
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
