// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React, { useEffect } from 'react';
import { useAppInstallStore } from '../stores/appInstallStore';
import { useUserStore } from '../stores/userStore';
import type { AppInstallUnified } from '../types';

const AppInstallsView: React.FC = () => {
  const {
    unifiedInstalls,
    fetchAll,
    accept,
    reject,
    createLicense,
    cancelInstall,
  } = useAppInstallStore();
  const { user, fetchUser } = useUserStore();

  useEffect(() => {
    fetchUser();
    fetchAll();
    const intervalId = setInterval(() => {
      fetchAll();
    }, 5000);
    return () => clearInterval(intervalId);
  }, [fetchUser, fetchAll]);

  return (
      <div>
        <h2>App Installs</h2>
        <div className="alert alert-info" role="alert">
          <strong>Note:</strong> Run <code>make create-app-install-request</code> to submit an AppInstallRequest
        </div>
        <div className="mt-4">
          <table className="table table-fixed" id="app-installs-table">
            <thead>
            <tr>
              <th style={{ width: '150px' }}>Contract ID</th>

              {user?.isAdmin && (
                <th style={{ width: '150px' }}>User</th>
              )}
              <th style={{ width: '100px' }}># Licenses</th>
              <th style={{ width: '300px' }}>Meta</th>
              <th style={{ width: '250px' }}>Status</th>              
              <th style={{ width: '310px' }}>Actions</th>
            </tr>
            </thead>
            <tbody>
            {unifiedInstalls.map((item: AppInstallUnified) => (
                <tr key={item.contractId} className="app-install-row">
                  <td className="ellipsis-cell app-install-contract-id">
                    {item.contractId}
                  </td>
                  {user?.isAdmin && (
                    <td className="ellipsis-cell app-install-user">
                      {item.user}
                    </td>
                  )}
                  <td className="app-install-num-licenses" data-testid="num-licenses">
                    {item.numLicensesCreated}
                  </td>
                  <td className="ellipsis-cell app-install-meta">
                    {item.meta ? JSON.stringify(item.meta.data) : '{}'}
                  </td>
                  <td className="ellipsis-cell app-install-status">
                    {item.status === 'REQUEST' ? 'AWAITING_ACCEPTANCE' : 'ACCEPTED'}
                  </td>                  
                  <td className="app-install-actions">
                    {item.status === 'REQUEST' ? (
                        user?.isAdmin ? (
                            <div className="btn-group" role="group">
                              {unifiedInstalls.findIndex((i) => i.status === 'INSTALL' && i.user === item.user) === -1 && (
                                <button
                                    className="btn btn-success btn-accept-install"
                                    onClick={() => accept(item.contractId, item.meta, {})}
                                >
                                  Accept
                                </button>
                              )}
                              <button
                                  className="btn btn-warning btn-reject-install"
                                  onClick={() => reject(item.contractId, {})}
                              >
                                Reject
                              </button>
                            </div>
                        ) : null
                    ) : (
                        <div className="btn-group" role="group">
                          <button
                              className="btn btn-danger btn-cancel-install"
                              onClick={() => cancelInstall(item.contractId, {})}
                          >
                            Cancel
                          </button>
                          {user?.isAdmin && (
                              <button
                                  className="btn btn-success btn-create-license"
                                  onClick={() => createLicense(item.contractId, {})}
                              >
                                Create License
                              </button>
                          )}
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