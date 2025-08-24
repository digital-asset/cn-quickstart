// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React, { useEffect, useState } from 'react';
import { useLicenseStore } from '../stores/licenseStore';
import { useUserStore } from '../stores/userStore';
import LicenseRenewalRequestModal from '../components/LicenseRenewalRequestModal.tsx';
import LicenseExpireModal from '../components/LicenseExpireModal.tsx';

import type {
  License,
  // LicenseRenewalRequest, // removed, managed inside RenewalsModal
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

  const [selectedLicenseId, setSelectedLicenseId] = useState<string | null>(null);
  const [selectedLicense, setSelectedLicense] = useState<License | null>(null);
  const [showExpireModal, setShowExpireModal] = useState(false);
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

  const closeModal = () => {
    setShowRenewalModal(false);
    setSelectedLicenseId(null);
  };

  const handleRenew = async (description: string) => {
    if (!selectedLicenseId || !selectedLicense) return;
    await initiateLicenseRenewal(selectedLicenseId, description);
    await fetchLicenses();
  };

  // Called when user confirms expiration from the LicenseExpireModal
  const handleExpire = async (description?: string) => {
    if (!selectedLicenseId) return;
    await initiateLicenseExpiration(selectedLicenseId, description!);
    setShowExpireModal(false);
    setSelectedLicenseId(null);
    await fetchLicenses();
  };

  const handleCompleteRenewal = async (renewalContractId: string, renewalRequestContractId: string, allocationContractId: string) => {
    await completeLicenseRenewal(renewalContractId, renewalRequestContractId, allocationContractId);
    await fetchLicenses();
  };

  const handleRenewalWithdraw = async (renewalContractId: string) => {
    if (!selectedLicenseId) return;
    await withdrawLicenseRenewalRequest(renewalContractId);
    setShowRenewalModal(false);
    setSelectedLicenseId(null);
    await fetchLicenses();
  };

  const formatDateTime = (iso?: string) => {
    if (!iso) return '';
    const d = new Date(iso);
    if (isNaN(d.getTime())) return iso;
    try {
    // const tz = Intl.DateTimeFormat().resolvedOptions().timeZone;
    return d.toLocaleString(undefined, {
      year: 'numeric',
      month: 'short',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
      // timeZoneName: 'short',
      // timeZone: tz,
    });
    } catch {
    return d.toString();
    }
  };

  const openExpireModal = (licenseId: string) => {
    setShowExpireModal(true);
    setSelectedLicenseId(licenseId);
  };

  const openRenewalModal = (licenseId: string) => {
    setShowRenewalModal(true);
    setSelectedLicenseId(licenseId);
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
              <td className="ellipsis-cell license-expires-at">{formatDateTime(license.expiresAt)}</td>
              <td className="ellipsis-cell license-number">{license.licenseNum}</td>
              <td className="ellipsis-cell">{license.renewalRequests?.filter(r => !r.allocationCid).length || 0}</td>
              <td className="ellipsis-cell">{license.renewalRequests?.filter(r => r.allocationCid).length || 0}</td>
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
                  {/* todo send isExpired from backend to avoid client clock skew */}
                  {license.expiresAt && new Date(license.expiresAt).getTime() < Date.now() && (
                    <button
                      className="btn btn-danger btn-expire-license"
                      onClick={() => openExpireModal(license.contractId)}
                    >
                      Expire
                    </button>
                  )}
              </td>
            </tr>
          );
        })}
        </tbody>
      </table>

      <LicenseRenewalRequestModal
        show={showRenewalModal && !!selectedLicenseId && !!selectedLicense}
        license={selectedLicense}
        onClose={closeModal}
        isAdmin={isAdmin}
        onIssueRenewal={handleRenew}
        onCompleteRenewal={handleCompleteRenewal}
        onWithdraw={handleRenewalWithdraw}
        formatDateTime={formatDateTime}
      />

      <LicenseExpireModal
        show={showExpireModal && !!selectedLicense}
        license={selectedLicense}
        isAdmin={isAdmin}
        onClose={() => {
          setShowExpireModal(false);
        }}
        onExpire={handleExpire}
      />
    </div>
  );
};

export default LicensesView;
