// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React, { useEffect, useState } from 'react';
import { useAppInstallStore } from '../stores/appInstallStore';
import { useUser } from '../stores/userStore';
import { useToast } from '../stores/toastStore';

const AppInstallsView: React.FC = () => {
    const {
        appInstalls,
        fetchUserInfo,
        fetchAppInstalls,
        cancelAppInstall,
        createLicenseFromAppInstall,
    } = useAppInstallStore();

    const { user } = useUser();
    const toast = useToast();

    const [selectedInstallId, setSelectedInstallId] = useState<string | null>(null);

    useEffect(() => {
        fetchUserInfo();
        fetchAppInstalls();
        const intervalId = setInterval(() => {
            fetchAppInstalls();
        }, 1000);
        return () => {
            clearInterval(intervalId);
        };
    }, [fetchUserInfo, fetchAppInstalls]);

    const handleCancel = async (contractId: string) => {
        await cancelAppInstall(contractId, {});
        toast.displaySuccess(`AppInstall with Contract ID ${contractId} was successfully canceled.`);
        setSelectedInstallId(null);
    };

    const handleCreateLicense = async (contractId: string) => {
        const result = await createLicenseFromAppInstall(contractId, {});
        if (result) {
            toast.displaySuccess(`License created with ID: ${result.licenseId}`);
        }
        setSelectedInstallId(null);
    };

    return (
        <div>
            <h2>App Installs</h2>

            <div className="mt-4">
                <h3>Existing AppInstalls</h3>
                <table className="table table-fixed">
                    <thead>
                    <tr>
                        <th style={{width: '150px'}}>Contract ID</th>
                        <th style={{width: '150px'}}>DSO</th>
                        <th style={{width: '150px'}}>Provider</th>
                        <th style={{width: '150px'}}>User</th>
                        <th style={{width: '200px'}}>Meta</th>
                        <th style={{width: '150px'}}>Num Licenses Created</th>
                        <th style={{width: '250px'}}>Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {appInstalls.map((install) => (
                        <tr key={install.contractId}>
                            <td className="ellipsis-cell">{install.contractId}</td>
                            <td className="ellipsis-cell">{install.payload.dso}</td>
                            <td className="ellipsis-cell">{install.payload.provider}</td>
                            <td className="ellipsis-cell">{install.payload.user}</td>
                            <td className="ellipsis-cell">{install.payload.meta ? JSON.stringify(install.payload.meta) : '{}'}</td>
                            <td>{install.payload.numLicensesCreated || 0}</td>
                            <td>
                                {selectedInstallId === install.contractId ? (
                                    <div>
                                        <div className="btn-group" role="group">
                                            {user?.isAdmin && (
                                                <button
                                                    className="btn btn-success"
                                                    onClick={() => handleCreateLicense(install.contractId)}
                                                >
                                                    Create License
                                                </button>
                                            )}
                                            <button
                                                className="btn btn-danger"
                                                onClick={() => handleCancel(install.contractId)}
                                            >
                                                Cancel Install
                                            </button>
                                            <button
                                                className="btn btn-secondary"
                                                onClick={() => {
                                                    setSelectedInstallId(null);
                                                }}
                                            >
                                                Close
                                            </button>
                                        </div>
                                    </div>
                                ) : (
                                    <button
                                        className="btn btn-primary"
                                        onClick={() => setSelectedInstallId(install.contractId)}
                                    >
                                        Actions
                                    </button>
                                )}
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default AppInstallsView;
