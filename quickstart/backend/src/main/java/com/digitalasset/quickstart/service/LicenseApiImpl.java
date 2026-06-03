// Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

// Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import static com.digitalasset.quickstart.service.ServiceUtils.ensurePresent;
import static com.digitalasset.quickstart.service.ServiceUtils.traceServiceCallAsync;
import static com.digitalasset.quickstart.utility.TracingUtils.tracingCtx;
import static com.digitalasset.quickstart.utility.Utils.*;

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
import io.opentelemetry.instrumentation.annotations.WithSpan;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.openapitools.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
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
            var licenseFut = damlRepository.findLicenseById(contractId);
            return registryAdminIdFut.thenCombine(licenseFut, (adminId, optLicense) -> {
                var license = ensurePresent(optLicense, "License not found for contract %s", contractId);
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
                        .<ResponseEntity<Void>>thenApply(r -> ResponseEntity.status(HttpStatus.CREATED).build());
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
                var choiceContext = ensurePresent(c, "Transfer context not found for allocation %s", request.getAllocationContractId());
                var renewal = ensurePresent(r, "Active renewal request not found for contract %s", request.getRenewalRequestContractId());
                TransferContext transferContext = prepareTransferContext(
                        choiceContext.getDisclosedContracts(),
                        choiceContext.getChoiceContextData()
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
                damlRepository.findLicenseById(contractId).thenCompose(optContract -> {
                    var license = ensurePresent(optContract, "License not found for contract %s", contractId);
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
            Object choiceContextData) {
        var disclosures = disclosedContracts
                .stream()
                .map(this::toLedgerApiDisclosedContract)
                .toList();
        return new TransferContext(
                new ExtraArgs(toChoiceContext(choiceContextData), toTokenStandarMetadata(Map.of())),
                disclosures
        );
    }

    @SuppressWarnings("unchecked")
    private static ChoiceContext toChoiceContext(Object choiceContextData) {
        if (choiceContextData == null) {
            return new ChoiceContext(Map.of());
        }
        if (!(choiceContextData instanceof Map)) {
            throw new IllegalArgumentException("Unexpected choiceContextData encoding: " + choiceContextData);
        }
        Object values = ((Map<String, Object>) choiceContextData).get("values");
        if (values == null) {
            return new ChoiceContext(Map.of());
        }
        if (!(values instanceof Map)) {
            throw new IllegalArgumentException("Unexpected choiceContextData.values encoding: " + values);
        }
        Map<String, AnyValue> context = new LinkedHashMap<>();
        ((Map<String, Object>) values).forEach((key, raw) -> context.put(key, toAnyValue(raw)));
        return new ChoiceContext(context);
    }

    @SuppressWarnings("unchecked")
    private static AnyValue toAnyValue(Object raw) {
        if (!(raw instanceof Map)) {
            throw new IllegalArgumentException("Unexpected AnyValue encoding: " + raw);
        }
        Map<String, Object> tagged = (Map<String, Object>) raw;
        String tag = (String) tagged.get("tag");
        Object value = tagged.get("value");
        if (tag == null) {
            throw new IllegalArgumentException("AnyValue is missing its tag: " + raw);
        }
        switch (tag) {
            case "AV_Text":
                return new AnyValue.AnyValue_AV_Text((String) value);
            case "AV_Int":
                return new AnyValue.AnyValue_AV_Int(Long.parseLong(value.toString()));
            case "AV_Decimal":
                return new AnyValue.AnyValue_AV_Decimal(new BigDecimal(value.toString()));
            case "AV_Bool":
                return new AnyValue.AnyValue_AV_Bool((Boolean) value);
            case "AV_ContractId":
                return toAnyValueContractId((String) value);
            case "AV_Party":
                return new AnyValue.AnyValue_AV_Party(new Party((String) value));
            case "AV_List":
                return new AnyValue.AnyValue_AV_List(
                        ((List<Object>) value).stream().map(LicenseApiImpl::toAnyValue).toList());
            case "AV_Map": {
                Map<String, AnyValue> entries = new LinkedHashMap<>();
                ((Map<String, Object>) value).forEach((k, v) -> entries.put(k, toAnyValue(v)));
                return new AnyValue.AnyValue_AV_Map(entries);
            }
            default:
                throw new IllegalArgumentException("Unsupported AnyValue tag: " + tag);
        }
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

    private static AnyValue toAnyValueContractId(String contractId) {
        return new AnyValue.AnyValue_AV_ContractId(new ContractId<>(contractId));
    }

    private static splice_api_token_metadata_v1.splice.api.token.metadatav1.Metadata toTokenStandarMetadata(Map<String, String> meta) {
        return new splice_api_token_metadata_v1.splice.api.token.metadatav1.Metadata(meta);
    }
}
