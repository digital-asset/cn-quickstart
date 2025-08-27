// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.repository;

import com.digitalasset.quickstart.pqs.Contract;
import com.digitalasset.quickstart.pqs.Pqs;
import com.digitalasset.transcode.java.ContractId;
import com.digitalasset.transcode.java.Template;
import com.digitalasset.transcode.java.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import quickstart_licensing.licensing.appinstall.AppInstall;
import quickstart_licensing.licensing.appinstall.AppInstallRequest;
import quickstart_licensing.licensing.license.License;
import quickstart_licensing.licensing.license.LicenseRenewalRequest;
import splice_api_token_allocation_request_v1.splice.api.token.allocationrequestv1.AllocationRequest;
import splice_api_token_allocation_v1.splice.api.token.allocationv1.Allocation;

/**
 * Repository for accessing active Daml contracts via PQS.
 */
@Repository
public class DamlRepository {

    private final Pqs pqs;

    @Autowired
    public DamlRepository(Pqs pqs) {
        this.pqs = pqs;
    }

    public static record LicenseRenewalRequestWithAllocationCid(
            Contract<LicenseRenewalRequest> renewal,
            Optional<ContractId<Allocation>> allocationCid) {
    }

    public static record LicenseWithRenewalRequests(
            Contract<License> license,
            List<LicenseRenewalRequestWithAllocationCid> renewals) {
    }

    private <T extends Template> T extractPayload(Class<T> clazz, String payload) {
        return clazz.cast(pqs.getJson2Dto().template(Utils.getTemplateIdByClass(clazz)).convert(payload));
    }

    private <T extends Template> Contract<T> extract(Class<T> clazz, ContractId<T> cid, String payload) {
        return new Contract<>(cid, extractPayload(clazz, payload));
    }

    private <T extends Template> Optional<ContractId<T>> optionalCid(Class<T> clazz, String cid) {
        return Optional.ofNullable(cid).map(ContractId<T>::new);
    }

    private <T extends Template> ContractId<T> cid(Class<T> clazz, String cid) {
        return new ContractId<T>(cid);
    }

    private <T extends Template> String qualifiedName(Class<T> clazz) {
        return Utils.getTemplateIdByClass(clazz).qualifiedName();
    }

    /**
     * Finds active License contracts where the user or provider matches the given party.
     */
    public CompletableFuture<List<LicenseWithRenewalRequests>> findActiveLicenses(String party) {
        var map = new HashMap<String, LicenseWithRenewalRequests>();
        String sql = """
                SELECT license.contract_id    AS license_contract_id,
                       license.payload        AS license_payload,
                       renewal.contract_id    AS renewal_contract_id,
                       renewal.payload        AS renewal_payload,
                       allocation.contract_id AS allocation_contract_id
                FROM active(?) license
                LEFT JOIN active(?) renewal ON
                    license.payload->>'licenseNum' = renewal.payload->>'licenseNum'
                    AND license.payload->>'user' = renewal.payload->>'user'
                LEFT JOIN active(?) allocation ON
                    renewal.payload->>'requestId' = allocation.payload->'allocation'->'settlement'->'settlementRef'->>'id'
                    AND renewal.payload->>'user' = allocation.payload->'allocation'->'transferLeg'->>'sender'
                WHERE license.payload->>'user' = ? OR license.payload->>'provider' = ?
                ORDER BY license.contract_id
                """;
        return pqs.query(sql, rs -> {
                    var licenseId = rs.getString("license_contract_id");
                    if (!map.containsKey(licenseId)) {
                        map.put(licenseId,
                                new LicenseWithRenewalRequests(
                                        extract(License.class, cid(License.class, licenseId), rs.getString("license_payload")),
                                        new java.util.ArrayList<>()
                                )
                        );
                    }
                    var renewalCid = optionalCid(LicenseRenewalRequest.class, rs.getString("renewal_contract_id"));
                    if (renewalCid.isPresent()) {
                        map.get(licenseId).renewals.add(new LicenseRenewalRequestWithAllocationCid(
                                        extract(LicenseRenewalRequest.class, renewalCid.get(), rs.getString("renewal_payload")),
                                        optionalCid(Allocation.class, rs.getString("allocation_contract_id"))
                                )
                        );
                    }
                },
                qualifiedName(License.class),
                qualifiedName(LicenseRenewalRequest.class),
                qualifiedName(Allocation.class),
                party,
                party
        ).thenApply(v -> new java.util.ArrayList<>(map.values()));
    }

    // KV we should be looking for active ones  only
    /**
     * Fetches a License contract by contract ID.
     */
    public CompletableFuture<Contract<License>> findLicenseById(String contractId) {
        return pqs.byContractId(License.class, contractId);
    }

    public <T extends Template> CompletableFuture<Optional<Contract<T>>> findActive(Class<T> clazz, String contractId) {
        return pqs.singleActiveWhere(clazz, "contract_id = ?", contractId);
    }

    public CompletableFuture<Optional<Contract<LicenseRenewalRequest>>> findActiveLicenseRenewalRequestById(String contractId) {
       return findActive(LicenseRenewalRequest.class, contractId);
    }

    public CompletableFuture<Optional<Contract<AllocationRequest>>> findActiveAllocationRequestById(String contractId) {
        return findActive(AllocationRequest.class, contractId);
    }

    /**
     * Finds all active AppInstall contracts.
     */
    public CompletableFuture<List<Contract<AppInstall>>> findActiveAppInstalls() {
        return pqs.active(AppInstall.class);
    }

    /**
     * Fetches an AppInstall contract by contract ID.
     */
    public CompletableFuture<Contract<AppInstall>> findAppInstallById(String contractId) {
        return pqs.byContractId(AppInstall.class, contractId);
    }

    /**
     * Finds all active AppInstallRequest contracts.
     */
    public CompletableFuture<List<Contract<AppInstallRequest>>> findActiveAppInstallRequests() {
        return pqs.active(AppInstallRequest.class);
    }

    /**
     * Fetches an AppInstallRequest contract by contract ID.
     */
    public CompletableFuture<Contract<AppInstallRequest>> findAppInstallRequestById(String contractId) {
        return pqs.byContractId(AppInstallRequest.class, contractId);
    }
}
