// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React, { useEffect, useState } from 'react';
import { useLicenseStore } from '../stores/licenseStore';
import { useUserStore } from '../stores/userStore';
import { useLocation } from 'react-router-dom';

/**
 * Renders a table of Licenses and provides actions to renew or expire them.
 */
const LicensesView: React.FC = () => {
    const {
        licenses,
        fetchLicenses,
        initiateLicenseRenewal,
        initiateLicenseExpiration,
        completeLicenseRenewal,
    } = useLicenseStore();

    const { user, fetchUser } = useUserStore();
    const location = useLocation();
    const isAdmin = !!user?.isAdmin;

    const [selectedLicenseId, setSelectedLicenseId] = useState<string | null>(null);
    const [renewDescription, setRenewDescription] = useState('');
    const [expireDescription, setExpireDescription] = useState('');

    useEffect(() => {
        fetchUser();
        fetchLicenses();
        const intervalId = setInterval(() => {
            fetchLicenses();
        }, 5000);
        return () => clearInterval(intervalId);
    }, [fetchUser, fetchLicenses]);

    const closeModal = () => {
        setSelectedLicenseId(null);
        setRenewDescription('');
        setExpireDescription('');
    };

    const handleRenew = async () => {
        if (!selectedLicenseId) return;
        await initiateLicenseRenewal(selectedLicenseId, renewDescription);
        closeModal();
    };

    const handleExpire = async () => {
        if (!selectedLicenseId) return;
        await initiateLicenseExpiration(selectedLicenseId, expireDescription);
        closeModal();
    };

    /**
     * Executes the final step of the renewal if a request exists and is already paid.
     */
    const handleCompleteRenewal = async (renewalContractId: string) => {
        await completeLicenseRenewal(renewalContractId);
        await fetchLicenses();
    };

    const currentURL = `${window.location.origin}${location.pathname}`;

    return (
        <div>
            <h2>Licenses</h2>
            <table className="table table-fixed">
                <thead>
                <tr>
                    <th style={{ width: '220px' }}>License Contract ID</th>
                    <th style={{ width: '100px' }}>DSO</th>
                    <th style={{ width: '120px' }}>Provider</th>
                    <th style={{ width: '120px' }}>User</th>
                    <th style={{ width: '180px' }}>Expires At</th>
                    <th style={{ width: '110px' }}>License #</th>
                    <th style={{ width: '100px' }}>Renew Fee</th>
                    <th style={{ width: '140px' }}>Extension</th>
                    <th style={{ width: '300px' }}>Actions</th>
                </tr>
                </thead>
                <tbody>
                {licenses.map((license) => {
                    const matchedRequest = license.renewalRequests?.find(
                        (req) =>
                            req.dso === license.dso &&
                            req.provider === license.provider &&
                            req.user === license.user &&
                            req.licenseNum === license.licenseNum
                    );

                    const fee = matchedRequest?.licenseFeeCc;
                    const extension = matchedRequest?.licenseExtensionDuration;
                    const payURL = matchedRequest
                        ? `${(user?.walletUrl || 'http://wallet.localhost:2000').replace(
                            /\/+$/,
                            ''
                        )}/confirm-payment/${matchedRequest.reference}?redirect=${encodeURIComponent(
                            currentURL
                        )}`
                        : '';

                    return (
                        <tr key={license.contractId}>
                            <td className="ellipsis-cell">{license.contractId}</td>
                            <td className="ellipsis-cell">{license.dso}</td>
                            <td className="ellipsis-cell">{license.provider}</td>
                            <td className="ellipsis-cell">{license.user}</td>
                            <td className="ellipsis-cell">{license.expiresAt}</td>
                            <td className="ellipsis-cell">{license.licenseNum}</td>
                            <td className="ellipsis-cell">{fee || ''}</td>
                            <td className="ellipsis-cell">{extension || ''}</td>
                            <td>
                                {matchedRequest ? (
                                    <>
                                        {/* If the user is NOT an admin: show Pay Renewal only if NOT paid */}
                                        {!isAdmin &&
                                            user &&
                                            matchedRequest.user === user.party &&
                                            !matchedRequest.isPaid && (
                                                <a
                                                    href={payURL}
                                                    className="btn btn-primary me-2"
                                                    target="_blank"
                                                    rel="noopener noreferrer"
                                                >
                                                    Pay Renewal
                                                </a>
                                            )}

                                        {/* If the user IS an admin: show Complete Renewal only if request IS paid */}
                                        {isAdmin && matchedRequest.isPaid && (
                                            <button
                                                className="btn btn-success"
                                                onClick={() =>
                                                    handleCompleteRenewal(matchedRequest.contractId)
                                                }
                                            >
                                                Complete Renewal
                                            </button>
                                        )}
                                    </>
                                ) : (
                                    // If there's no existing renewal, only Admin sees the Actions button
                                    isAdmin && (
                                        <button
                                            className="btn btn-primary"
                                            onClick={() => setSelectedLicenseId(license.contractId)}
                                        >
                                            Actions
                                        </button>
                                    )
                                )}
                            </td>
                        </tr>
                    );
                })}
                </tbody>
            </table>

            {/* Modal for Renew/Expire actions (Admin only) */}
            {selectedLicenseId && (
                <>
                    <div className="modal-backdrop fade show"></div>
                    <div className="modal show d-block" tabIndex={-1}>
                        <div className="modal-dialog modal-lg">
                            <div className="modal-content">
                                <div className="modal-header">
                                    <h5 className="modal-title">
                                        Actions for License {selectedLicenseId.substring(0, 24)}
                                    </h5>
                                    <button
                                        type="button"
                                        className="btn-close"
                                        aria-label="Close"
                                        onClick={closeModal}
                                    ></button>
                                </div>
                                <div className="modal-body">
                                    <div className="mb-4">
                                        <h6>Renew License</h6>
                                        <p>
                                            <strong>Extension:</strong> 30 days (P30D),{' '}
                                            <strong>Payment Acceptance:</strong> 7 days (P7D),{' '}
                                            <strong>Fee:</strong> 100 CC
                                        </p>
                                        <label>Description:</label>
                                        <input
                                            className="form-control mb-2"
                                            placeholder='e.g. "Renew for next month"'
                                            value={renewDescription}
                                            onChange={(e) => setRenewDescription(e.target.value)}
                                        />
                                        <button
                                            className="btn btn-success"
                                            onClick={handleRenew}
                                            disabled={!renewDescription.trim()}
                                        >
                                            Issue Renewal Payment Request
                                        </button>
                                    </div>
                                    <hr />
                                    <div className="mb-4">
                                        <h6>Expire License</h6>
                                        <label>Description:</label>
                                        <input
                                            className="form-control mb-2"
                                            placeholder='e.g. "License expired"'
                                            value={expireDescription}
                                            onChange={(e) => setExpireDescription(e.target.value)}
                                        />
                                        <button
                                            className="btn btn-danger"
                                            onClick={handleExpire}
                                            disabled={!expireDescription.trim()}
                                        >
                                            Expire
                                        </button>
                                    </div>
                                </div>
                                <div className="modal-footer">
                                    <button className="btn btn-secondary" onClick={closeModal}>
                                        Close
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </>
            )}
        </div>
    );
};

export default LicensesView;
