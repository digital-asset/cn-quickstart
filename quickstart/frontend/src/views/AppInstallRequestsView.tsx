// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React, { useEffect, useState } from 'react';
import { useAppInstallRequestStore } from '../stores/appInstallRequestStore';
import { useUser } from '../stores/userStore';

const AppInstallRequestsView: React.FC = () => {
    const {
        appInstallRequests,
        fetchUserInfo,
        fetchAppInstallRequests,
        acceptAppInstallRequest,
        rejectAppInstallRequest,
        cancelAppInstallRequest,
    } = useAppInstallRequestStore();

    const { user } = useUser();
    const [selectedRequestId, setSelectedRequestId] = useState<string | null>(null);

    useEffect(() => {
        fetchUserInfo();
        fetchAppInstallRequests();
        const intervalId = setInterval(() => {
            fetchAppInstallRequests();
        }, 1000);
        return () => {
            clearInterval(intervalId);
        };
    }, [fetchUserInfo, fetchAppInstallRequests]);

    const handleAccept = async (contractId: string) => {
        await acceptAppInstallRequest(contractId, {}, {});
        setSelectedRequestId(null);
    };

    const handleReject = async (contractId: string) => {
        await rejectAppInstallRequest(contractId, {});
        setSelectedRequestId(null);
    };

    const handleCancel = async (contractId: string) => {
        await cancelAppInstallRequest(contractId, {});
        setSelectedRequestId(null);
    };

    return (
        <div>
            <div className="alert alert-info" role="alert">
                <strong>Note:</strong> Run <code>make create-app-install-request</code> to submit an AppInstallRequest
                from the AppUser participant.
            </div>

            <h2>App Install Requests</h2>

            <div className="mt-4">
                <table className="table table-fixed">
                    <thead>
                    <tr>
                        <th style={{ width: '150px' }}>Contract ID</th>
                        <th style={{ width: '150px' }}>DSO</th>
                        <th style={{ width: '150px' }}>Provider</th>
                        <th style={{ width: '150px' }}>User</th>
                        <th style={{ width: '200px' }}>Meta</th>
                        <th style={{ width: '200px' }}>Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {appInstallRequests.map((request) => (
                        <tr key={request.contractId}>
                            <td className="ellipsis-cell">{request.contractId}</td>
                            <td className="ellipsis-cell">{request.payload.dso}</td>
                            <td className="ellipsis-cell">{request.payload.provider}</td>
                            <td className="ellipsis-cell">{request.payload.user}</td>
                            <td className="ellipsis-cell">
                                {request.payload.meta ? JSON.stringify(request.payload.meta) : '{}'}
                            </td>
                            <td>
                                {selectedRequestId === request.contractId ? (
                                    <div>
                                        <div className="btn-group" role="group">
                                            {user?.isAdmin && (
                                                <>
                                                    <button
                                                        className="btn btn-success"
                                                        onClick={() => handleAccept(request.contractId)}
                                                    >
                                                        Accept
                                                    </button>
                                                    <button
                                                        className="btn btn-warning"
                                                        onClick={() => handleReject(request.contractId)}
                                                    >
                                                        Reject
                                                    </button>
                                                </>
                                            )}
                                            <button
                                                className="btn btn-danger"
                                                onClick={() => handleCancel(request.contractId)}
                                            >
                                                Cancel
                                            </button>
                                            <button
                                                className="btn btn-secondary"
                                                onClick={() => {
                                                    setSelectedRequestId(null);
                                                }}
                                            >
                                                Close
                                            </button>
                                        </div>
                                    </div>
                                ) : (
                                    <button
                                        className="btn btn-primary"
                                        onClick={() => setSelectedRequestId(request.contractId)}
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

export default AppInstallRequestsView;
