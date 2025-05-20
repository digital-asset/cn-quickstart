// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.repository;

import com.digitalasset.quickstart.pqs.Contract;
import com.digitalasset.quickstart.pqs.Pqs;
import com.digitalasset.transcode.java.ContractId;
import com.digitalasset.transcode.java.Template;
import com.digitalasset.transcode.java.Utils;
import com.digitalasset.transcode.schema.Identifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import quickstart_licensing.licensing.license.License;
import quickstart_licensing.licensing.license.LicenseRenewalRequest;
import splice_api_token_allocation_v1.splice.api.token.allocationv1.Allocation;
import splice_wallet_payments.splice.wallet.payment.AcceptedAppPayment;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Repository for accessing active Daml contracts via PQS.
 */
@Repository
public class DamlRepository {

    private final Pqs pqs;

    /**
     * Represents a LicenseRenewalRequest contract paired with a flag indicating whether it is paid.
     */
    public static record LicenseRenewalRequestData(Contract<LicenseRenewalRequest> contract, boolean isPaid) {
    }

    @Autowired
    public DamlRepository(Pqs pqs) {
        this.pqs = pqs;
    }

    /**
     * Finds active License contracts where the user or provider matches the given party.
     */
    public CompletableFuture<List<Contract<License>>> findActiveLicensesByParty(String party) {
        String whereClause = "payload->>'user' = ? OR payload->>'provider' = ?";
        return pqs.activeWhere(License.class, whereClause, party, party);
    }

    /**
     * Finds active LicenseRenewalRequest contracts alongside a flag indicating if each request is paid.
     */
    public CompletableFuture<List<LicenseRenewalRequestData>> findActiveLicenseRenewalRequestsByParty(String party) {
        String joinCondition = """
                    prim.payload->>'reference' = sec.payload->>'reference'
                    AND prim.payload->>'user' = sec.payload->>'sender'
                    AND prim.payload->>'provider' = sec.payload->>'provider'
                """;
        String whereClause = "prim.payload->>'user' = ? OR prim.payload->>'provider' = ?";

        return pqs.activeLeftJoinWhere(
                LicenseRenewalRequest.class,
                AcceptedAppPayment.class,
                joinCondition,
                whereClause,
                (rs, rowNum) -> {
                    Contract<LicenseRenewalRequest> contract = new Contract<>(
                            new ContractId<>(rs.getString("primary_contract_id")),
                            (LicenseRenewalRequest) pqs.getJson2Dto()
                                    .template(Utils.getTemplateIdByClass(LicenseRenewalRequest.class))
                                    .convert(rs.getString("primary_payload"))
                    );
                    boolean isPaid = rs.getString("secondary_contract_id") != null;
                    return new LicenseRenewalRequestData(contract, isPaid);
                },
                party,
                party
        );
    }

    /**
     * Fetches a LicenseRenewalRequest contract by contract ID.
     */
    public CompletableFuture<Contract<LicenseRenewalRequest>> findLicenseRenewalRequestById(String contractId) {
        return pqs.byContractId(LicenseRenewalRequest.class, contractId);
    }

    /**
     * Finds all active License contracts (no filters).
     */
    public CompletableFuture<List<Contract<License>>> findAllActiveLicenses() {
        return pqs.active(License.class);
    }

    /**
     * Fetches a License contract by contract ID.
     */
    public CompletableFuture<Contract<License>> findLicenseById(String contractId) {
        return pqs.byContractId(License.class, contractId);
    }

    /**
     * Fetches a single active License matching all given fields.
     */
    public CompletableFuture<Optional<Contract<License>>> findSingleActiveLicense(
            String user,
            String provider,
            Long licenseNum,
            String dso
    ) {
        String whereClause =
                "payload->>'user' = ? "
                        + "AND payload->>'provider' = ? "
                        + "AND (payload->>'licenseNum')::int = ? "
                        + "AND payload->>'dso' = ?";

        return pqs.singleActiveWhere(License.class, whereClause, user, provider, licenseNum, dso);
    }


    /**
     * Finds the active allocation for a given renewal request reference
     */
    public CompletableFuture<Optional<Contract<Allocation>>> findActiveAllocationForRenewalRequest(String referenceCid) {
        String whereClause = "payload->>'reference' = ?";

        return pqs.singleActiveWhere(Allocation.class, whereClause, referenceCid);
    }


    /**
     * Finds all active AppInstall contracts.
     */
    public CompletableFuture<List<Contract<quickstart_licensing.licensing.appinstall.AppInstall>>> findActiveAppInstalls() {
        return pqs.active(quickstart_licensing.licensing.appinstall.AppInstall.class);
    }

    /**
     * Fetches an AppInstall contract by contract ID.
     */
    public CompletableFuture<Contract<quickstart_licensing.licensing.appinstall.AppInstall>> findAppInstallById(String contractId) {
        return pqs.byContractId(quickstart_licensing.licensing.appinstall.AppInstall.class, contractId);
    }

    /**
     * Finds all active AppInstallRequest contracts.
     */
    public CompletableFuture<List<Contract<quickstart_licensing.licensing.appinstall.AppInstallRequest>>> findActiveAppInstallRequests() {
        return pqs.active(quickstart_licensing.licensing.appinstall.AppInstallRequest.class);
    }

    /**
     * Fetches an AppInstallRequest contract by contract ID.
     */
    public CompletableFuture<Contract<quickstart_licensing.licensing.appinstall.AppInstallRequest>> findAppInstallRequestById(String contractId) {
        return pqs.byContractId(quickstart_licensing.licensing.appinstall.AppInstallRequest.class, contractId);
    }
}
