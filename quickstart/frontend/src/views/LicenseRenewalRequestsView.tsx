// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React, { useEffect } from 'react';
import { useLicenseStore } from '../stores/licenseStore';
import { useLocation } from 'react-router-dom';
import { useUser } from '../stores/userStore';
import {Int64, Party} from "../../generated/daml";

const LicenseRenewalRequestsView: React.FC = () => {
    const {
        licenseRenewalRequests,
        fetchLicenseRenewalRequests,
        licenses,
        fetchLicenses,
        completeLicenseRenewal,
    } = useLicenseStore();
    const { user } = useUser();
    const location = useLocation();

    useEffect(() => {
        fetchLicenseRenewalRequests();
        fetchLicenses();
        const intervalId = setInterval(() => {
            fetchLicenseRenewalRequests();
            fetchLicenses();
        }, 5000);
        return () => clearInterval(intervalId);
    }, [fetchLicenseRenewalRequests, fetchLicenses]);

    const currentURL = `${window.location.origin}${location.pathname}`;

    const handleCompleteRenewal = async (
        requestContractId: string,
        reference: string,
        dso: Party,
        provider: Party,
        requestUser: Party,
        licenseNum: Int64
    ) => {
        // Find the matching license (assuming it still exists)
        const matchingLicense = licenses.find(
            (l) =>
                l.payload.dso === dso &&
                l.payload.provider === provider &&
                l.payload.user === requestUser &&
                l.payload.licenseNum === licenseNum
        );

        if (!matchingLicense) {
            alert("Matching license not found. Cannot complete renewal.");
            return;
        }

        const requestBody = {
            reference: reference,
            licenseCid: matchingLicense.contractId,
        };

        await completeLicenseRenewal(requestContractId, requestBody);
        // After completion, we might refresh lists
        await fetchLicenseRenewalRequests();
        await fetchLicenses();
    };

    return (
        <div>
            <h2>License Renewal Requests</h2>
            {licenseRenewalRequests.length === 0 ? (
                <p>No License Renewal Requests found.</p>
            ) : (
                <table className="table table-fixed">
                    <thead>
                    <tr>
                        <th style={{ width: '200px' }}>Contract ID</th>
                        <th style={{ width: '150px' }}>Provider</th>
                        <th style={{ width: '150px' }}>User</th>
                        <th style={{ width: '150px' }}>DSO</th>
                        <th style={{ width: '150px' }}>License Number</th>
                        <th style={{ width: '150px' }}>Fee (CC)</th>
                        <th style={{ width: '200px' }}>Extension Duration</th>
                        <th style={{ width: '300px' }}>Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {licenseRenewalRequests.map((request) => {
                        const payURL = `http://wallet.localhost:2000/confirm-payment/${request.payload.reference}?redirect=${encodeURIComponent(currentURL)}`;
                        return (
                            <tr key={request.contractId}>
                                <td className="ellipsis-cell">{request.contractId}</td>
                                <td className="ellipsis-cell">{request.payload.provider}</td>
                                <td className="ellipsis-cell">{request.payload.user}</td>
                                <td className="ellipsis-cell">{request.payload.dso}</td>
                                <td className="ellipsis-cell">{request.payload.licenseNum}</td>
                                <td className="ellipsis-cell">{request.payload.licenseFeeCc}</td>
                                <td className="ellipsis-cell">{request.payload.licenseExtensionDuration.microseconds}</td>
                                <td>
                                    {user && request.payload.user === user.party && (
                                        <a
                                            href={payURL}
                                            className="btn btn-primary me-2"
                                            target="_blank"
                                            rel="noopener noreferrer"
                                        >
                                            Pay
                                        </a>
                                    )}
                                    {user && user.isAdmin && (
                                        <button
                                            className="btn btn-success"
                                            onClick={() =>
                                                handleCompleteRenewal(
                                                    request.contractId,
                                                    request.payload.reference,
                                                    request.payload.dso,
                                                    request.payload.provider,
                                                    request.payload.user,
                                                    request.payload.licenseNum
                                                )
                                            }
                                        >
                                            Complete Renewal
                                        </button>
                                    )}
                                </td>
                            </tr>
                        );
                    })}
                    </tbody>
                </table>
            )}
        </div>
    );
};

export default LicenseRenewalRequestsView;
