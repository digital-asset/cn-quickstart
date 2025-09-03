// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React, { useEffect, useState } from 'react';
import { useLicenseStore } from '../stores/licenseStore';
import { useUserStore } from '../stores/userStore';
import LicenseRenewalRequestModal from '../components/LicenseRenewalRequestModal.tsx';
import LicenseArchiveModal from '../components/LicenseExpireModal.tsx';
import { formatDateTime } from '../utils/format';

import type {
  License,
  LicenseRenewRequest,
} from '../openapi.d.ts';

const LicensesView: React.FC = () => {
  const {
    licenses,
    fetchLicenses,
    initiateLicenseRenewal,
    initiateLicenseExpiration,
    completeLicenseRenewal,
    withdrawLicenseRenewalRequest
  } = useLicenseStore();

  const { user, fetchUser } = useUserStore();
  const isAdmin = !!user?.isAdmin;
  const userWallet = user?.walletUrl || 'http://wallet.localhost:2000';

  const [selectedLicenseId, setSelectedLicenseId] = useState<string | null>(null);
  const [selectedLicense, setSelectedLicense] = useState<License | null>(null);
  const [showArchiveModal, setShowArchiveModal] = useState(false);
  const [showRenewalModal, setShowRenewalModal] = useState(false);

  useEffect(() => {
    fetchUser();
    fetchLicenses();
    const intervalId = setInterval(() => {
      fetchLicenses();
    }, 5000);
    return () => clearInterval(intervalId);
  }, [fetchUser, fetchLicenses]);

  useEffect(() => {
    if (!selectedLicenseId) {
      setSelectedLicense(null);
      return;
    }
    setSelectedLicense(licenses.find(l => l.contractId === selectedLicenseId) ?? null);
  }, [licenses, selectedLicenseId]);

  const openArchiveModal = (licenseId: string) => {
    setShowArchiveModal(true);
    setSelectedLicenseId(licenseId);
  };

  const handleArchive = async (description?: string) => {
    if (!selectedLicenseId) return;
    await initiateLicenseExpiration(selectedLicenseId, description!);
    setShowArchiveModal(false);
    setSelectedLicenseId(null);
    await fetchLicenses();
  };

  const closeArchiveModal = () => {
    setShowArchiveModal(false);
    setSelectedLicenseId(null);
  };

  const openRenewalModal = (licenseId: string) => {
    setShowRenewalModal(true);
    setSelectedLicenseId(licenseId);
  };

  const handleCompleteRenewal = async (renewalContractId: string, renewalRequestContractId: string, allocationContractId: string) => {
    const result = await completeLicenseRenewal(renewalContractId, renewalRequestContractId, allocationContractId);
    if (result) {
      setSelectedLicenseId(result.licenseId!);
    }
    await fetchLicenses();
  };

  const handleRenewalWithdraw = async (renewalContractId: string) => {
    if (!selectedLicenseId) return;
    await withdrawLicenseRenewalRequest(renewalContractId);
    await fetchLicenses();
  };

  const closeRenewalsModal = () => {
    setShowRenewalModal(false);
    setSelectedLicenseId(null);
  };

  const handleRenew = async (request: LicenseRenewRequest) => {
    if (!selectedLicenseId || !selectedLicense) return;
    await initiateLicenseRenewal(selectedLicenseId, request);
    await fetchLicenses();
  };


  return (
    <div>
      <h2>Licenses</h2>
      <table className="table table-fixed" id="licenses-table">
        <thead>
        <tr>
          <th style={{ width: '220px' }}>License Contract ID</th>
          {user?.isAdmin && (
            <th style={{ width: '150px' }}>User</th>
          )}
          <th style={{ width: '200px' }}>Expires At</th>
          <th style={{ width: '110px' }}>License #</th>
          <th style={{ width: '100px' }}>Pending Renewals</th>
          <th style={{ width: '100px' }}>Accepted Renewals</th>
          <th style={{ width: '130px' }}>Status</th>          
          <th style={{ width: '300px' }}>Actions</th>
        </tr>
        </thead>
        <tbody>
        {licenses.map((license) => {
          return (
            <tr key={license.contractId} className="license-row">
              <td className="ellipsis-cell license-contract-id">{license.contractId}</td>
              {user?.isAdmin && (
                <td className="ellipsis-cell license-user">{license.user}</td>
              )}
              <td className={`ellipsis-cell license-expires-at ${license.isExpired && 'deadline-passed'}`}>
                {formatDateTime(license.expiresAt)}
              </td>
              <td className="ellipsis-cell license-number">{license.licenseNum}</td>
              <td className="ellipsis-cell">{license.renewalRequests?.filter(r => !r.allocationCid).length || 0}</td>
              <td className="ellipsis-cell">{license.renewalRequests?.filter(r => r.allocationCid).length || 0}</td>
              <td className="ellipsis-cell license-status">{license.isExpired ? 'EXPIRED' : 'ACTIVE'}</td>
              <td className="license-actions">
                  {(isAdmin || (license.renewalRequests?.length ?? 0) > 0) && (
                    <button
                      className="btn btn-primary btn-actions-license"
                      onClick={() => openRenewalModal(license.contractId)}
                    >
                      Renewals
                    </button>
                    )
                  }
                  {license.expiresAt && license.isExpired && (
                    <button
                      className="btn btn-danger btn-expire-license"
                      onClick={() => openArchiveModal(license.contractId)}
                    >
                      Archive
                    </button>
                  )}
              </td>
            </tr>
          );
        })}
        </tbody>
      </table>

      <LicenseRenewalRequestModal
        show={showRenewalModal && !!selectedLicense}
        license={selectedLicense}
        onClose={closeRenewalsModal}
        isAdmin={isAdmin}
        userWallet={userWallet}
        onIssueRenewal={handleRenew}
        onCompleteRenewal={handleCompleteRenewal}
        onWithdraw={handleRenewalWithdraw}
        formatDateTime={formatDateTime}
      />

      <LicenseArchiveModal
        show={showArchiveModal && !!selectedLicense}
        license={selectedLicense}
        onClose={closeArchiveModal}
        onArchive={handleArchive}
      />
    </div>
  );
};

export default LicensesView;
