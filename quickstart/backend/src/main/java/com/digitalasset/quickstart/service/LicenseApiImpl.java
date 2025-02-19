// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.api.LicensesApi;
import com.digitalasset.quickstart.ledger.LedgerApi;
import com.digitalasset.quickstart.oauth.AuthenticatedPartyService;
import com.digitalasset.quickstart.repository.DamlRepository;
import com.digitalasset.quickstart.utility.LoggingSpanHelper;
import com.digitalasset.transcode.java.ContractId;
import com.digitalasset.transcode.java.Party;
import daml_prim_da_types.da.types.Tuple2;
import daml_stdlib_da_time_types.da.time.types.RelTime;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.openapitools.model.LicenseExpireRequest;
import org.openapitools.model.LicenseRenewRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import quickstart_licensing.licensing.license.License.License_Expire;
import quickstart_licensing.licensing.license.License.License_Renew;
import quickstart_licensing.licensing.license.LicenseRenewalRequest;
import quickstart_licensing.licensing.util.Metadata;
import splice_wallet_payments.splice.wallet.payment.AppPaymentRequest;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.digitalasset.quickstart.utility.ContextAwareCompletableFutures.completeWithin;
import static com.digitalasset.quickstart.utility.ContextAwareCompletableFutures.supplyWithin;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class LicenseApiImpl implements LicensesApi {

    private static final Logger logger = LoggerFactory.getLogger(LicenseApiImpl.class);

    private final LedgerApi ledger;
    private final DamlRepository damlRepository;
    private final AuthenticatedPartyService authenticatedPartyService;

    @Autowired
    public LicenseApiImpl(
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
    public CompletableFuture<ResponseEntity<String>> expireLicense(
            @SpanAttribute("license.contractId") String contractId,
            @SpanAttribute("license.commandId") String commandId,
            LicenseExpireRequest licenseExpireRequest
    ) {
        Span methodSpan = Span.current();
        Context parentContext = Context.current();

        Map<String, Object> attributes = Map.of(
                "license.contractId", contractId,
                "license.commandId", commandId,
                "templateId", quickstart_licensing.licensing.license.License.TEMPLATE_ID.qualifiedName(),
                "choiceName", "License_Expire"
        );

        LoggingSpanHelper.addEventWithAttributes(methodSpan, "Starting expireLicense", attributes);
        LoggingSpanHelper.setSpanAttributes(methodSpan, attributes);
        LoggingSpanHelper.logInfo(logger, "expireLicense: received request", attributes);

        return authenticatedPartyService.getPartyOrFail()
                .thenCompose(actingParty ->
                        CompletableFuture.supplyAsync(
                                supplyWithin(parentContext, () -> {
                                    LoggingSpanHelper.addEventWithAttributes(methodSpan, "Exercising License_Expire on contract", attributes);
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
                                })
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

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<List<org.openapitools.model.License>>> listLicenses() {
        Span methodSpan = Span.current();
        Context parentContext = Context.current();

        Map<String, Object> startAttributes = Map.of(
                "templateId", "quickstart_licensing.licensing.license.License"
        );

        LoggingSpanHelper.addEventWithAttributes(methodSpan, "Starting listLicenses", startAttributes);
        LoggingSpanHelper.setSpanAttributes(methodSpan, startAttributes);
        LoggingSpanHelper.logInfo(
                logger,
                "listLicenses: received request, fetching party for filtering of licenses",
                startAttributes
        );

        return authenticatedPartyService.getPartyOrFail()
                .thenCompose(party ->
                        CompletableFuture.supplyAsync(
                                supplyWithin(parentContext, () -> {
                                    Map<String, Object> filterAttributes = Map.of("party", party);
                                    LoggingSpanHelper.addEventWithAttributes(
                                            methodSpan,
                                            "Fetching and filtering licenses by current party",
                                            filterAttributes
                                    );
                                    LoggingSpanHelper.logDebug(logger, "Filtering licenses", filterAttributes);

                                    return damlRepository.findActiveLicenses()
                                            .thenApply(contracts -> {
                                                List<org.openapitools.model.License> result = contracts.stream()
                                                        .filter(contract -> {
                                                            String user = contract.payload.getUser.getParty;
                                                            String provider = contract.payload.getProvider.getParty;
                                                            return party.equals(user) || party.equals(provider);
                                                        })
                                                        .map(contract -> {
                                                            org.openapitools.model.License l = new org.openapitools.model.License();
                                                            l.setContractId(contract.contractId.getContractId);
                                                            l.setDso(contract.payload.getDso.getParty);
                                                            l.setProvider(contract.payload.getProvider.getParty);
                                                            l.setUser(contract.payload.getUser.getParty);

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
                                            });
                                })
                        ).thenCompose(cf -> cf)
                )
                .whenComplete(
                        completeWithin(parentContext, (res, ex) -> {
                            if (ex == null) {
                                int count = (res.getBody() != null) ? res.getBody().size() : 0;
                                Map<String, Object> successAttributes = Map.of("count", count);
                                LoggingSpanHelper.logDebug(logger, "listLicenses: success", successAttributes);
                            } else {
                                LoggingSpanHelper.logError(logger, "listLicenses: failed", startAttributes, ex);
                                LoggingSpanHelper.recordException(methodSpan, ex);
                            }
                        })
                );
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
                "templateId", quickstart_licensing.licensing.license.License.TEMPLATE_ID.qualifiedName(),
                "choiceName", "License_Renew"
        );

        LoggingSpanHelper.addEventWithAttributes(methodSpan, "Starting renewLicense", attributes);
        LoggingSpanHelper.setSpanAttributes(methodSpan, attributes);
        LoggingSpanHelper.logInfo(logger, "renewLicense: received request", attributes);

        return authenticatedPartyService.getPartyOrFail()
                .thenCompose(providerParty ->
                        CompletableFuture.supplyAsync(
                                supplyWithin(parentContext, () -> {
                                    LoggingSpanHelper.addEventWithAttributes(methodSpan, "Exercising License_Renew on contract", attributes);

                                    return damlRepository.findLicenseById(contractId)
                                            .thenCompose(contract -> {
                                                Duration extDuration = Duration.parse(licenseRenewRequest.getLicenseExtensionDuration());
                                                long extensionMicros = extDuration.toNanos() / 1000;
                                                RelTime licenseExtensionDuration = new RelTime(extensionMicros);

                                                Duration payDuration = Duration.parse(licenseRenewRequest.getPaymentAcceptanceDuration());
                                                long payDurationMicros = payDuration.toNanos() / 1000;
                                                RelTime paymentAcceptanceDuration = new RelTime(payDurationMicros);

                                                License_Renew choice = new License_Renew(
                                                        licenseRenewRequest.getLicenseFeeCc(),
                                                        licenseExtensionDuration,
                                                        paymentAcceptanceDuration,
                                                        licenseRenewRequest.getDescription()
                                                );

                                                return ledger.exerciseAndGetResult(providerParty, contract.contractId, choice, commandId)
                                                        .thenApply((Tuple2<ContractId<LicenseRenewalRequest>, ContractId<AppPaymentRequest>> result) -> {
                                                            Map<String, Object> successAttributes = new HashMap<>(attributes);
                                                            successAttributes.put("renewalRequestCid", result.get_1.getContractId);
                                                            successAttributes.put("paymentRequestCid", result.get_2.getContractId);
                                                            LoggingSpanHelper.logInfo(logger, "License renewed", successAttributes);
                                                            return ResponseEntity.ok().<Void>build();
                                                        });
                                            });
                                })
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
}
