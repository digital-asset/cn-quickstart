// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React, { useEffect } from 'react';
import { useLicenseStore } from '../stores/licenseStore';
import { useLocation } from 'react-router-dom';
import { useUserStore } from '../stores/userStore';

const LicenseRenewalRequestsView: React.FC = () => {
    const {
        licenseRenewalRequests,
        fetchLicenseRenewalRequests,
        licenses,
        fetchLicenses,
        completeLicenseRenewal,
    } = useLicenseStore();
    const { user } = useUserStore();
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

    // Current page URL to pass as a redirect parameter:
    const currentURL = `${window.location.origin}${location.pathname}`;

    const handleCompleteRenewal = async (
        requestContractId: string,
        dso: string,
        provider: string,
        requestUser: string,
        licenseNum: number
    ) => {
        // Find the matching license (assuming it still exists)
        const matchingLicense = licenses.find(
            (l) =>
                l.dso === dso &&
                l.provider === provider &&
                l.user === requestUser &&
                l.licenseNum === licenseNum
        );

        if (!matchingLicense) {
            alert("Matching license not found. Cannot complete renewal.");
            return;
        }

        await completeLicenseRenewal(requestContractId);
        // After completion, refresh the data
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
                        // Safely handle the walletUrl (remove trailing slash, etc.)
                        const baseWalletUrl = user?.walletUrl
                            ? user.walletUrl.replace(/\/+$/, '') // remove trailing slash
                            : 'http://wallet.localhost:2000'; // fallback if needed

                        const payURL = `${baseWalletUrl}/confirm-payment/${request.reference}?redirect=${encodeURIComponent(currentURL)}`;

                        return (
                            <tr key={request.contractId}>
                                <td className="ellipsis-cell">{request.contractId}</td>
                                <td className="ellipsis-cell">{request.provider}</td>
                                <td className="ellipsis-cell">{request.user}</td>
                                <td className="ellipsis-cell">{request.dso}</td>
                                <td className="ellipsis-cell">{request.licenseNum}</td>
                                <td className="ellipsis-cell">{request.licenseFeeCc}</td>
                                <td className="ellipsis-cell">{request.licenseExtensionDuration}</td>
                                <td>
                                    {/* Render Pay button only if current user is the one who needs to pay */}
                                    {user && request.user === user.party && (
                                        <a
                                            href={payURL}
                                            className="btn btn-primary me-2"
                                            target="_blank"
                                            rel="noopener noreferrer"
                                        >
                                            Pay
                                        </a>
                                    )}
                                    {/* Render admin button if user is admin */}
                                    {user && user.isAdmin && (
                                        <button
                                            className="btn btn-success"
                                            onClick={() =>
                                                handleCompleteRenewal(
                                                    request.contractId,
                                                    request.dso,
                                                    request.provider,
                                                    request.user,
                                                    request.licenseNum
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
