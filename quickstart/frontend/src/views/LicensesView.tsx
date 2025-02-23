// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React, { useEffect, useState } from 'react';
import { useLicenseStore } from '../stores/licenseStore';
import { useUser } from '../stores/userStore'; // Importing the user store

const LicensesView: React.FC = () => {
    const {
        licenses,
        fetchUserInfo,
        fetchLicenses,
        initiateLicenseRenewal,
        initiateLicenseExpiration,
    } = useLicenseStore();

    const { user } = useUser(); // Getting the current user
    const isAdmin = !!user?.isAdmin; // Determine if user is admin

    const [selectedLicenseId, setSelectedLicenseId] = useState<string | null>(null);
    const [renewDescription, setRenewDescription] = useState('');
    const [expireDescription, setExpireDescription] = useState('');

    useEffect(() => {
        fetchUserInfo();
        fetchLicenses();
        const intervalId = setInterval(() => {
            fetchLicenses();
        }, 2000);
        return () => clearInterval(intervalId);
    }, [fetchUserInfo, fetchLicenses]);

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

    const closeModal = () => {
        setSelectedLicenseId(null);
        setRenewDescription('');
        setExpireDescription('');
    };

    return (
        <div>
            <h2>Licenses</h2>
            <div className="mt-4">
                <h3>Existing Licenses</h3>
                <table className="table table-fixed">
                    <thead>
                    <tr>
                        <th style={{ width: '200px' }}>Contract ID</th>
                        <th style={{ width: '150px' }}>DSO</th>
                        <th style={{ width: '150px' }}>Provider</th>
                        <th style={{ width: '150px' }}>User</th>
                        <th style={{ width: '200px' }}>Params</th>
                        <th style={{ width: '200px' }}>Expires At</th>
                        <th style={{ width: '100px' }}>License Num</th>
                        {isAdmin && (
                            <th style={{ width: '200px' }}>Actions</th>
                        )}
                    </tr>
                    </thead>
                    <tbody>
                    {licenses.map((license) => (
                        <tr key={license.contractId}>
                            <td className="ellipsis-cell">{license.contractId}</td>
                            <td className="ellipsis-cell">{license.dso}</td>
                            <td className="ellipsis-cell">{license.provider}</td>
                            <td className="ellipsis-cell">{license.user}</td>
                            <td className="ellipsis-cell">{JSON.stringify(license.params)}</td>
                            <td className="ellipsis-cell">{license.expiresAt}</td>
                            <td className="ellipsis-cell">{license.licenseNum}</td>
                            {isAdmin && (
                                <td>
                                    <button
                                        className="btn btn-primary"
                                        onClick={() => {
                                            setSelectedLicenseId(license.contractId);
                                        }}
                                    >
                                        Actions
                                    </button>
                                </td>
                            )}
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>

            {selectedLicenseId && (
                <>
                    {/* Backdrop behind the modal */}
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
                                            Renew
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
