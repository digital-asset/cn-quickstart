// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React, { useEffect, useState } from 'react';
import { useAppInstallStore } from '../stores/appInstallStore';
import { useUserStore } from '../stores/userStore';
import type { AppInstallUnified } from '../types';

const AppInstallsView: React.FC = () => {
  const {
    unifiedInstalls,
    fetchAll,
    accept,
    reject,
    cancelRequest,
    cancelInstall,
    createLicense,
  } = useAppInstallStore();

  const { user, fetchUser } = useUserStore();
  const [selectedId, setSelectedId] = useState<string | null>(null);

  useEffect(() => {
    fetchUser();
    fetchAll();
    const intervalId = setInterval(() => {
      fetchAll();
    }, 1000);
    return () => clearInterval(intervalId);
  }, [fetchUser, fetchAll]);

  const handleAccept = async (contractId: string) => {
    await accept(contractId, {}, {});
    setSelectedId(null);
  };

  const handleReject = async (contractId: string) => {
    await reject(contractId, {});
    setSelectedId(null);
  };

  const handleCancelRequest = async (contractId: string) => {
    await cancelRequest(contractId, {});
    setSelectedId(null);
  };

  const handleCancelInstall = async (contractId: string) => {
    await cancelInstall(contractId, {});
    setSelectedId(null);
  };

  const handleCreateLicense = async (contractId: string) => {
    await createLicense(contractId, {});
    setSelectedId(null);
  };

  const renderActions = (item: AppInstallUnified) => {
    if (selectedId !== item.contractId) {
      return (
          <button
              className="btn btn-primary"
              onClick={() => setSelectedId(item.contractId)}
          >
            Actions
          </button>
      );
    }

    if (item.status === 'REQUEST') {
      return (
          <div className="btn-group" role="group">
            {user?.isAdmin && (
                <>
                  <button className="btn btn-success" onClick={() => handleAccept(item.contractId)}>
                    Accept
                  </button>
                  <button className="btn btn-warning" onClick={() => handleReject(item.contractId)}>
                    Reject
                  </button>
                </>
            )}
            <button className="btn btn-danger" onClick={() => handleCancelRequest(item.contractId)}>
              Cancel
            </button>
            <button className="btn btn-secondary" onClick={() => setSelectedId(null)}>
              Close
            </button>
          </div>
      );
    }

    // For installs
    return (
        <div className="btn-group" role="group">
          {user?.isAdmin && (
              <button
                  className="btn btn-success"
                  onClick={() => handleCreateLicense(item.contractId)}
              >
                Create License
              </button>
          )}
          <button className="btn btn-danger" onClick={() => handleCancelInstall(item.contractId)}>
            Cancel Install
          </button>
          <button className="btn btn-secondary" onClick={() => setSelectedId(null)}>
            Close
          </button>
        </div>
    );
  };

  return (
      <div>
        <h2>App Installs</h2>

        <div className="alert alert-info" role="alert">
          <strong>Note:</strong> Run <code>make create-app-install-request</code> to submit an AppInstallRequest
        </div>

        <div className="mt-4">
          <table className="table table-fixed">
            <thead>
            <tr>
              <th style={{ width: '150px' }}>Contract ID</th>
              <th style={{ width: '80px' }}>Status</th>
              <th style={{ width: '150px' }}>DSO</th>
              <th style={{ width: '150px' }}>Provider</th>
              <th style={{ width: '150px' }}>User</th>
              <th style={{ width: '200px' }}>Meta</th>
              <th style={{ width: '180px' }}># Licenses Created</th>
              <th style={{ width: '250px' }}>Actions</th>
            </tr>
            </thead>
            <tbody>
            {unifiedInstalls.map((item) => (
                <tr key={item.contractId}>
                  <td className="ellipsis-cell">{item.contractId}</td>
                  <td className="ellipsis-cell">{item.status}</td>
                  <td className="ellipsis-cell">{item.dso}</td>
                  <td className="ellipsis-cell">{item.provider}</td>
                  <td className="ellipsis-cell">{item.user}</td>
                  <td className="ellipsis-cell">
                    {item.meta ? JSON.stringify(item.meta) : '{}'}
                  </td>
                  <td>{item.numLicensesCreated}</td>
                  <td>{renderActions(item)}</td>
                </tr>
            ))}
            </tbody>
          </table>
        </div>
      </div>
  );
};

export default AppInstallsView;
