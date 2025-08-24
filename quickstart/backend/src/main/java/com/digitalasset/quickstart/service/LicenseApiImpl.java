// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import static com.digitalasset.quickstart.utility.LoggingSpanHelper.*;

import com.daml.ledger.api.v2.CommandsOuterClass;
import com.daml.ledger.api.v2.ValueOuterClass;
import com.digitalasset.quickstart.api.LicensesApi;
import com.digitalasset.quickstart.ledger.LedgerApi;
import com.digitalasset.quickstart.ledger.TokenStandardProxy;
import com.digitalasset.quickstart.repository.DamlRepository;
import com.digitalasset.quickstart.security.AuthUtils;
import com.digitalasset.quickstart.tokenstandard.openapi.allocation.model.DisclosedContract;
import com.digitalasset.transcode.java.ContractId;
import com.digitalasset.transcode.java.Party;
import com.google.protobuf.ByteString;
import daml_stdlib_da_time_types.da.time.types.RelTime;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.openapitools.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import quickstart_licensing.licensing.license.License.License_Expire;
import quickstart_licensing.licensing.license.License.License_Renew;
import quickstart_licensing.licensing.license.LicenseRenewalRequest.LicenseRenewalRequest_CompleteRenewal;
import splice_api_token_holding_v1.splice.api.token.holdingv1.InstrumentId;
import splice_api_token_metadata_v1.splice.api.token.metadatav1.AnyValue;
import splice_api_token_metadata_v1.splice.api.token.metadatav1.ChoiceContext;
import splice_api_token_metadata_v1.splice.api.token.metadatav1.ExtraArgs;

/**
 * License management service for handling contract-based operations on
 * Licenses.
 */
@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class LicenseApiImpl implements LicensesApi {

    private static final Logger logger = LoggerFactory.getLogger(LicenseApiImpl.class);

    private final LedgerApi ledger;
    private final DamlRepository damlRepository;
    private final TokenStandardProxy tokenStandardProxy;
    private final AuthUtils auth;

    public LicenseApiImpl(
            LedgerApi ledger,
            DamlRepository damlRepository,
            TokenStandardProxy tokenStandardProxy,
            AuthUtils authUtils) {
        this.ledger = ledger;
        this.damlRepository = damlRepository;
        this.tokenStandardProxy = tokenStandardProxy;
        this.auth = authUtils;
    }

    /**
     * Lists active License contracts visible to the authenticated party.
     */
    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<List<License>>> listLicenses() {
        return auth.asAuthenticatedParty(party -> traceWithStartEventAsync(
                logger,
                "listLicenses",
                Map.of(),
                () -> damlRepository.findActiveLicenses(party).thenApply(res -> res.stream().map(l -> {
                            var license = l.license();
                            License licenseApi = new License();
                            licenseApi.setContractId(license.cid().getContractId);
                            licenseApi.setProvider(license.payload().getProvider.getParty);
                            licenseApi.setUser(license.payload().getUser.getParty);
                            var metaApi = new Metadata();
                            var paramsApi = new LicenseParams();
                            licenseApi.setParams(paramsApi.meta(metaApi.data(license.payload().getParams.getMeta.getValues)));
                            licenseApi.setExpiresAt(toOffsetDateTime(license.payload().getExpiresAt));
                            licenseApi.setLicenseNum(license.payload().getLicenseNum.intValue());
                            var renewalsApi = l.renewals().stream().map(r -> {
                                var renewalApi = new LicenseRenewalRequest();
                                renewalApi.setContractId(r.renewal().cid().getContractId);
                                renewalApi.setProvider(r.renewal().payload().getProvider.getParty);
                                renewalApi.setUser(r.renewal().payload().getUser.getParty);
                                renewalApi.setLicenseNum(r.renewal().payload().getLicenseNum.intValue());
                                renewalApi.setLicenseFeeAmount(r.renewal().payload().getLicenseFeeAmount);
                                renewalApi.setDescription(r.renewal().payload().getDescription);
                                renewalApi.setPrepareUntil(toOffsetDateTime(r.renewal().payload().getPrepareUntil));
                                renewalApi.setSettleBefore(toOffsetDateTime(r.renewal().payload().getSettleBefore));
                                renewalApi.setRequestedAt(toOffsetDateTime(r.renewal().payload().getRequestedAt));
                                renewalApi.setRequestId(r.renewal().payload().getRequestId);
                                long micros = r.renewal().payload().getLicenseExtensionDuration.getMicroseconds;
                                String approximateDays = (micros / 1_000_000 / 3600 / 24) + " days";
                                renewalApi.setLicenseExtensionDuration(approximateDays);
                                if (r.allocationCid().isPresent()) {
                                    renewalApi.setAllocationCid(r.allocationCid().get().getContractId);
                                }
                                return renewalApi;
                            }).toList();
                            licenseApi.setRenewalRequests(renewalsApi.stream().sorted(Comparator.comparing(LicenseRenewalRequest::getRequestedAt)).toList());
                            return licenseApi;
                        }).sorted(Comparator.comparing(License::getUser)
                                .thenComparingInt(License::getLicenseNum)).toList())
                        .thenApply(ResponseEntity::ok)
        ));
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<Void>> renewLicense(
            @SpanAttribute("license.contractId") String contractId,
            @SpanAttribute("license.commandId") String commandId,
            LicenseRenewRequest request) {
        return auth.asAdminParty(party -> traceWithStartEventAsync(
                logger,
                "renewLicense",
                Map.of(
                        "contractId", contractId,
                        "commandId", commandId
                ),
                () -> {
                    var registryAdminIdFut = tokenStandardProxy.getRegistryAdminId();
                    var licenseFut = damlRepository.findLicenseById(contractId);
                    return registryAdminIdFut.thenCombine(licenseFut, Pair::of).thenCompose(registryAdminIdAndLicensePair -> {
                        License_Renew choice = new License_Renew(UUID.randomUUID().toString(),
                                new InstrumentId(new Party(registryAdminIdAndLicensePair.first), "Amulet"),
                                request.getLicenseFeeCc(),
                                parseRelTime(request.getLicenseExtensionDuration()),
                                parseRelTime(request.getPaymentAcceptanceDuration()),
                                Instant.now(),
                                Instant.now().plus(1, ChronoUnit.HOURS), Instant.now().plus(2, ChronoUnit.HOURS),
                                request.getDescription()
                        );
                        return ledger.exerciseAndGetResult(registryAdminIdAndLicensePair.second.contractId, choice, commandId)
                                .thenApply(r -> ResponseEntity.ok().build());
                    });
                })
        );
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<Void>> completeLicenseRenewal(
            @SpanAttribute("licenseRenewal.contractId") String contractId,
            @SpanAttribute("licenseRenewal.commandId") String commandId,
            CompleteLicenseRenewalRequest request) {
        return auth.asAdminParty(party -> traceWithStartEventAsync(
                logger,
                "completeLicenseRenewal",
                Map.of(
                        "contractId", contractId,
                        "commandId", commandId
                ),
                () -> tokenStandardProxy.getAllocationTransferContext(request.getAllocationContractId()).thenCompose(choiceContext -> {
                    var disclosures = choiceContext.getDisclosedContracts()
                            .stream()
                            .map(this::toLedgerApiDisclosedContract)
                            .toList();
                    Map<String, AnyValue> choiceContextMap = disclosures
                            .stream()
                            .map(dc -> {
                                var entityName = dc.getTemplateId().getEntityName();
                                if (entityName.equals("AmuletRules")) {
                                    return Pair.of("amulet-rules", toAnyValueContractId(dc.getContractId()));
                                } else if (entityName.equals("OpenMiningRound")) {
                                    return Pair.of("open-round", toAnyValueContractId(dc.getContractId()));
                                } else {
                                    return null;
                                }
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toMap(Pair::first, Pair::second));
                    LicenseRenewalRequest_CompleteRenewal choice = new LicenseRenewalRequest_CompleteRenewal(
                            new ContractId<>(request.getAllocationContractId()),
                            new ContractId<>(contractId),
                            new ExtraArgs(new ChoiceContext(choiceContextMap), toTokenStandarMetadata(Map.of()))
                    );
                    return ledger.exerciseAndGetResult(new ContractId<>(request.getRenewalRequestContractId()), choice, commandId, disclosures)
                            .thenApply(newLicenseCid -> {
                                logger.info("newLicenseContractId: {}", newLicenseCid.getContractId);
                                return ResponseEntity.ok().build();
                            });
                })
        ));
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<String>> expireLicense(
            @SpanAttribute("contractId") String contractId,
            @SpanAttribute("commandId") String commandId,
            LicenseExpireRequest licenseExpireRequest) {
        return auth.asAuthenticatedParty(party -> traceWithStartEventAsync(
                logger,
                "expireLicense",
                Map.of(
                        "contractId", contractId,
                        "commandId", commandId
                ),
                () -> damlRepository.findLicenseById(contractId).thenCompose(contract -> {
                    var meta = licenseExpireRequest.getMeta().getData();
                    if (!party.equals(auth.getAppProviderPartyId())) {
                        meta = new HashMap<>(meta);
                        meta.put("Note", "Triggered by user request");
                    }
                    License_Expire choice = new License_Expire(new Party(auth.getAppProviderPartyId()), toTokenStandarMetadata(meta));
                    return ledger.exerciseAndGetResult(contract.contractId, choice, commandId)
                            .thenApply(result -> ResponseEntity.ok("License expired successfully"));
                }))
        );
    }

    private CommandsOuterClass.DisclosedContract toLedgerApiDisclosedContract(DisclosedContract dc) {
        ValueOuterClass.Identifier templateId = parseTemplateIdentifier(dc.getTemplateId());
        byte[] blob = Base64.getDecoder().decode(dc.getCreatedEventBlob());

        return CommandsOuterClass.DisclosedContract.newBuilder().setTemplateId(templateId).setContractId(dc.getContractId())
                .setCreatedEventBlob(ByteString.copyFrom(blob)).build();
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

        return ValueOuterClass.Identifier.newBuilder().setPackageId(packageId).setModuleName(moduleName)
                .setEntityName(entityName).build();
    }

    record Pair<A, B>(A first, B second) {
        static <A, B> Pair<A, B> of(A a, B b) {
            return new Pair<>(a, b);
        }
    }

    private AnyValue toAnyValueContractId(String contractId) {
        return new AnyValue.AnyValue_AV_ContractId(new ContractId<>(contractId));
    }

    private static RelTime parseRelTime(String durationStr) {
        Duration duration = Duration.parse(durationStr);
        long micros = duration.toNanos() / 1_000;
        return new RelTime(micros);
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static splice_api_token_metadata_v1.splice.api.token.metadatav1.Metadata toTokenStandarMetadata(Map<String, String> meta) {
        return new splice_api_token_metadata_v1.splice.api.token.metadatav1.Metadata(meta);
    }
}
