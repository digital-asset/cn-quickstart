// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React, { useEffect } from 'react';
import { useAppInstallStore } from '../stores/appInstallStore';
import { useUserStore } from '../stores/userStore';
import type { AppInstallUnified } from '../types';

/**
 * Displays a list of AppInstallUnified items with always-visible action buttons.
 */
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

  useEffect(() => {
    fetchUser();
    fetchAll();
    const intervalId = setInterval(() => {
      fetchAll();
    }, 1000);
    return () => clearInterval(intervalId);
  }, [fetchUser, fetchAll]);

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
              <th style={{ width: '100px' }}>Status</th>
              <th style={{ width: '150px' }}>DSO</th>
              <th style={{ width: '150px' }}>Provider</th>
              <th style={{ width: '150px' }}>User</th>
              <th style={{ width: '200px' }}>Meta</th>
              <th style={{ width: '100px' }}># Licenses</th>
              <th style={{ width: '310px' }}>Actions</th>
            </tr>
            </thead>
            <tbody>
            {unifiedInstalls.map((item: AppInstallUnified) => (
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
                  <td>
                    {item.status === 'REQUEST' ? (
                        <div className="btn-group" role="group">
                          {user?.isAdmin && (
                              <>
                                <button
                                    className="btn btn-success"
                                    onClick={() => accept(item.contractId, {}, {})}
                                >
                                  Accept
                                </button>
                                <button
                                    className="btn btn-warning"
                                    onClick={() => reject(item.contractId, {})}
                                >
                                  Reject
                                </button>
                              </>
                          )}
                          <button
                              className="btn btn-danger"
                              onClick={() => cancelRequest(item.contractId, {})}
                          >
                            Cancel
                          </button>
                        </div>
                    ) : (
                        <div className="btn-group" role="group">
                          {user?.isAdmin && (
                              <button
                                  className="btn btn-success"
                                  onClick={() => createLicense(item.contractId, {})}
                              >
                                Create License
                              </button>
                          )}
                          <button
                              className="btn btn-danger"
                              onClick={() => cancelInstall(item.contractId, {})}
                          >
                            Cancel Install
                          </button>
                        </div>
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

