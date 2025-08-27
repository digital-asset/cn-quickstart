// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import static com.digitalasset.quickstart.service.ServiceUtils.ensurePresent;
import static com.digitalasset.quickstart.service.ServiceUtils.traceServiceCallAsync;
import static com.digitalasset.quickstart.utility.TracingUtils.tracingCtx;

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
import io.opentelemetry.instrumentation.annotations.WithSpan;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
 * Management service for handling contract-based operations on Licenses.
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
            AuthUtils authUtils
    ) {
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
        var ctx = tracingCtx(logger, "listLicenses");
        return auth.asAuthenticatedParty(party -> traceServiceCallAsync(ctx, () ->
                damlRepository.findActiveLicenses(party).thenApply(res -> res.stream()
                        .map(LicenseApiImpl::toLicenseApi)
                        .sorted(Comparator.comparing(License::getUser).thenComparingInt(License::getLicenseNum))
                        .toList()
                ).thenApply(ResponseEntity::ok)
        ));
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<Void>> renewLicense(
            String contractId,
            String commandId,
            LicenseRenewRequest request
    ) {
        var ctx = tracingCtx(logger, "renewLicense",
                "contractId", contractId,
                "commandId", commandId
        );
        return auth.asAdminParty(party -> traceServiceCallAsync(ctx, () -> {
            var registryAdminIdFut = tokenStandardProxy.getRegistryAdminId();
            var licenseFut = damlRepository.findLicenseById(contractId); // KV fix rework to return Optional
            return registryAdminIdFut.thenCombine(licenseFut, (adminId, license) -> {
                var now = Instant.now();
                License_Renew choice = new License_Renew(
                        UUID.randomUUID().toString(),
                        new InstrumentId(new Party(adminId), "Amulet"),
                        request.getLicenseFeeCc(),
                        parseRelTime(request.getLicenseExtensionDuration()),
                        now,
                        now.plus(Duration.parse(request.getPrepareUntilDuration())),
                        now.plus(Duration.parse(request.getSettleBeforeDuration())),
                        request.getDescription()
                );
                return ledger.exerciseAndGetResult(license.contractId, choice, commandId)
                        .thenApply(r -> ResponseEntity.ok().<Void>build());
            }).thenCompose(x -> x);
        }));
    }


    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<LicenseRenewalResult>> completeLicenseRenewal(
            String contractId,
            String commandId,
            CompleteLicenseRenewalRequest request
    ) {
        var ctx = tracingCtx(logger, "completeLicenseRenewal",
                "contractId", contractId,
                "commandId", commandId
        );
        return auth.asAdminParty(party -> traceServiceCallAsync(ctx, () -> {
            var choiceContextFut = tokenStandardProxy.getAllocationTransferContext(request.getAllocationContractId());
            var renewalFut = damlRepository.findActiveLicenseRenewalRequestById(request.getRenewalRequestContractId());
            return choiceContextFut.thenCombine(renewalFut, (c, r) -> {
                var choiceContext = ensurePresent(c, "Transfer context not found for allocation {}", request.getAllocationContractId());
                var renewal = ensurePresent(r, "Active renewal request not found for contract {}", request.getRenewalRequestContractId());
                TransferContext transferContext = prepareTransferContext(
                        choiceContext.getDisclosedContracts(),
                        Map.of(
                                "AmuletRules", "amulet-rules",
                                "OpenMiningRound", "open-round"
                        )
                );
                LicenseRenewalRequest_CompleteRenewal choice = new LicenseRenewalRequest_CompleteRenewal(
                        new ContractId<>(request.getAllocationContractId()),
                        new ContractId<>(contractId),
                        transferContext.extraArgs
                );
                return ledger.exerciseAndGetResult(renewal.contractId, choice, commandId, transferContext.disclosedContracts)
                        .thenApply(newLicenseCid -> {
                            logger.info("newLicenseContractId: {}", newLicenseCid.getContractId);
                            LicenseRenewalResult result = new LicenseRenewalResult();
                            result.setLicenseId(newLicenseCid.getContractId);
                            return ResponseEntity.ok(result);
                        });
            }).thenCompose(x -> x);
        }));
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<String>> expireLicense(
            String contractId,
            String commandId,
            LicenseExpireRequest licenseExpireRequest
    ) {
        var ctx = tracingCtx(logger, "expireLicense",
                "contractId", contractId,
                "commandId", commandId
        );
        return auth.asAuthenticatedParty(party -> traceServiceCallAsync(ctx, () ->
                damlRepository.findLicenseById(contractId).thenCompose(contract -> { // KV make it return Optional
                    var license = ensurePresent(Optional.ofNullable(contract), "License not found for contract {}", contractId);
                    var meta = licenseExpireRequest.getMeta().getData();
                    if (!party.equals(auth.getAppProviderPartyId())) {
                        meta = new HashMap<>(meta);
                        meta.put("Note", "Triggered by user request");
                    }
                    License_Expire choice = new License_Expire(new Party(auth.getAppProviderPartyId()), toTokenStandarMetadata(meta));
                    return ledger.exerciseAndGetResult(license.contractId, choice, commandId)
                            .thenApply(result -> ResponseEntity.ok("License expired successfully"));
                })
        ));
    }

    private static License toLicenseApi(DamlRepository.LicenseWithRenewalRequests licenseContract) {
        var lp = licenseContract.license().payload;
        var now = Instant.now();
        License licenseApi = new License();
        licenseApi.setContractId(licenseContract.license().contractId.getContractId);
        licenseApi.setProvider(lp.getProvider.getParty);
        licenseApi.setUser(lp.getUser.getParty);
        licenseApi.setIsExpired(!lp.getExpiresAt.isAfter(now));
        var metaApi = new Metadata();
        var paramsApi = new LicenseParams();
        licenseApi.setParams(paramsApi.meta(metaApi.data(lp.getParams.getMeta.getValues)));
        licenseApi.setExpiresAt(toOffsetDateTime(lp.getExpiresAt));
        licenseApi.setLicenseNum(lp.getLicenseNum.intValue());
        var renewalsApi = licenseContract.renewals().stream().map(renewalContract ->
                toLicenseRenewalRequestAPI(renewalContract, now)
        ).sorted(Comparator.comparing(LicenseRenewalRequest::getRequestedAt)).toList();
        licenseApi.setRenewalRequests(renewalsApi);
        return licenseApi;
    }

    private static LicenseRenewalRequest toLicenseRenewalRequestAPI(DamlRepository.LicenseRenewalRequestWithAllocationCid renewalContract, Instant now) {
        var rp = renewalContract.renewal().payload;
        var renewalApi = new LicenseRenewalRequest();
        renewalApi.setContractId(renewalContract.renewal().contractId.getContractId);
        renewalApi.setProvider(rp.getProvider.getParty);
        renewalApi.setUser(rp.getUser.getParty);
        renewalApi.setLicenseNum(rp.getLicenseNum.intValue());
        renewalApi.setLicenseFeeAmount(rp.getLicenseFeeAmount);
        renewalApi.setDescription(rp.getDescription);
        renewalApi.setPrepareUntil(toOffsetDateTime(rp.getPrepareUntil));
        renewalApi.setSettleBefore(toOffsetDateTime(rp.getSettleBefore));
        renewalApi.setRequestedAt(toOffsetDateTime(rp.getRequestedAt));
        renewalApi.setRequestId(rp.getRequestId);
        long micros = rp.getLicenseExtensionDuration.getMicroseconds;
        String approximateDays = (micros / 1_000_000 / 3600 / 24) + " days";
        renewalApi.setLicenseExtensionDuration(approximateDays);
        renewalApi.setPrepareDeadlinePassed(!rp.getPrepareUntil.isAfter(now));
        renewalApi.setSettleDeadlinePassed(!rp.getSettleBefore.isAfter(now));
        if (renewalContract.allocationCid().isPresent()) {
            renewalApi.setAllocationCid(renewalContract.allocationCid().get().getContractId);
        }
        return renewalApi;
    }

    private record TransferContext(ExtraArgs extraArgs, List<CommandsOuterClass.DisclosedContract> disclosedContracts) {
    }

    private TransferContext prepareTransferContext(
            List<DisclosedContract> disclosedContracts,
            Map<String, String> metaMap) {
        var disclosures = disclosedContracts
                .stream()
                .map(this::toLedgerApiDisclosedContract)
                .toList();
        Map<String, AnyValue> choiceContextMap = disclosures
                .stream()
                .map(dc -> {
                    var metaKey = metaMap.get(dc.getTemplateId().getEntityName());
                    if (metaKey != null) {
                        return Map.entry(metaKey, toAnyValueContractId(dc.getContractId()));
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new TransferContext(
                new ExtraArgs(new ChoiceContext(choiceContextMap), toTokenStandarMetadata(Map.of())),
                disclosures
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
