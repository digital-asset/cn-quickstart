import { useState } from 'react';
import Modal from './Modal.tsx';
import type { License, LicenseRenewalRequest } from '../openapi';

type Props = {
  show: boolean;
  license: License | null;
  onClose: () => void;
  isAdmin: boolean;
  onIssueRenewal: (description: string) => Promise<void> | void;
  onCompleteRenewal: (licenseContractId: string, renewalContractId: string, allocationCid: string) => Promise<void> | void;
  onWithdraw: (renewalContractId: string) => Promise<void> | void;
  formatDateTime: (iso?: string) => string;
};

export default function LicenseRenewalRequestModal({
  show,
  license,
  onClose,
  isAdmin,
  onIssueRenewal,
  onCompleteRenewal,
  onWithdraw,
  formatDateTime,
}: Props) {
  const [renewDescription, setRenewDescription] = useState('');
  const [selectedRenewal, setSelectedRenewal] = useState<LicenseRenewalRequest | null>(null);
  const [showAcceptModal, setShowAcceptModal] = useState(false);
  const [showRejectModal, setShowRejectModal] = useState(false);

  return (
    <Modal
      show={show}
      title={
        <div><strong>License Contract ID:</strong> {license?.contractId.substring(0, 24)}...</div>
      }
      onClose={() => {
        setSelectedRenewal(null);
        setRenewDescription('');
        onClose();
      }}
      backdrop="static"
      size="xl"
      zIndexBase={1500}
      dialogClassName="auto-width-modal"
      contentClassName="auto-width-content"
    >
      {/* Accept nested modal (portals to body; z-index above the Renewals modal) */}
      <Modal
        show={showAcceptModal && !!selectedRenewal}
        title="Accept Allocation Request"
        confirmButtonTitle="OK"
        onClose={() => {
          setShowAcceptModal(false);
          setSelectedRenewal(null);
        }}
        backdrop="static"
        size="lg"
        zIndexBase={2000}
      >
        {selectedRenewal && (
          <p>
            Please accept the allocation request with reference{' '}
            <strong>{selectedRenewal.requestId}</strong> from{' '}
            <strong>{selectedRenewal.provider}</strong> in your wallet.
          </p>
        )}
      </Modal>

      <Modal
        show={showRejectModal && !!selectedRenewal}
        title="Reject Allocation Request"
        confirmButtonTitle="OK"
        onClose={() => {
          setShowRejectModal(false);
          setSelectedRenewal(null);
        }}
        backdrop="static"
        size="lg"
        zIndexBase={2000}
      >
        {selectedRenewal && (
          <p style={{ whiteSpace: 'pre-wrap' }}>
            Please exercise choice <strong>AllocationRequest_Reject</strong> on the <strong>AllocationRequest</strong>{' '}
            on the <strong>app-user's participant</strong> to reject the <strong>LicenseRenewalRequest</strong>.
          </p>
        )}
      </Modal>


      {isAdmin && (
        <div className="mb-4">
          <h6>Renew License</h6>
          <p>
            <strong>Extension:</strong> 30 days (P30D),{' '}
            <strong>Payment Acceptance:</strong> 7 days (P7D),{' '}
            <strong>Fee:</strong> 100 CC
          </p>
          <label>Description:</label>
          <input
            className="form-control mb-2 input-renew-description"
            placeholder='e.g. "Renew for next month"'
            value={renewDescription}
            onChange={(e) => setRenewDescription(e.target.value)}
          />
          <button
            className="btn btn-success btn-issue-renewal"
            onClick={async () => {
              if (!renewDescription.trim()) return;
              await onIssueRenewal(renewDescription);
              setRenewDescription('');
            }}
            disabled={!renewDescription.trim()}
          >
            Issue Renewal Payment Request
          </button>
        </div>
      )}

      <div className="renewals">
        <h2>Renewals</h2>
        <table className="table table-fixed xtable-bordered" id="renewals-table">
          <thead>
            <tr>
              <th style={{ width: '110px' }}>Renewal Contract ID</th>
              <th style={{ width: '110px' }}>Request Id</th>
              <th style={{ width: '100px' }}>Requested At</th>
              <th style={{ width: '50px' }}>Extension</th>
              <th style={{ width: '30px' }}>Fee</th>
              <th style={{ width: '100px' }}>Prepare Until</th>
              <th style={{ width: '100px' }}>Settle Before</th>
              <th style={{ width: '70px' }}>Description</th>
              <th style={{ width: '100px' }}>Allocation Id</th>
              <th style={{ width: '220px' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {license?.renewalRequests?.map((renewal) => {
              return (
                <tr key={renewal.contractId} className="renewal-row">
                  <td className="ellipsis-cell renewal-contract-id">{renewal.contractId}</td>
                  <td className="ellipsis-cell renewal-request-id">{renewal.requestId}</td>
                  <td className="ellipsis-cell renewal-requested-at">{formatDateTime(renewal.requestedAt)}</td>
                  <td className="ellipsis-cell">{renewal.licenseExtensionDuration}</td>
                  <td className="ellipsis-cell">{renewal.licenseFeeAmount}</td>
                  <td className="ellipsis-cell">{formatDateTime(renewal.prepareUntil)}</td>
                  <td className="ellipsis-cell">{formatDateTime(renewal.settleBefore)}</td>
                  <td className="ellipsis-cell">{renewal.description}</td>
                  <td className="ellipsis-cell">{renewal.allocationCid}</td>
                  <td className="license-actions">
                    <>
                      {!isAdmin && !renewal.allocationCid && (
                        <button
                          className="btn btn-success btn-accept"
                          onClick={() => {
                            setSelectedRenewal(renewal);
                            setShowAcceptModal(true);
                          }}
                        >
                          Accept
                        </button>
                      )}
                      {isAdmin && renewal.allocationCid && license && (
                        <button
                          className="btn btn-success btn-complete-renewal"
                          onClick={() =>
                            onCompleteRenewal(
                              license.contractId,
                              renewal.contractId,
                              renewal.allocationCid!
                            )
                          }
                        >
                          Complete Renewal
                        </button>
                      )}
                    </>
                    {isAdmin && renewal && (
                      <button
                        className="btn btn-warning btn-withdraw"
                        onClick={() => {
                          onWithdraw(renewal.contractId);
                        }}
                      >
                        Withdraw
                      </button>
                    )}
                    {!isAdmin && renewal && (
                      <button
                        className="btn btn-danger btn-expire-reject"
                          onClick={() => {
                            setSelectedRenewal(renewal);
                            setShowRejectModal(true);
                          }}
                      >
                        Reject
                      </button>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      {/* Footer omitted to use Modal's default Close button */}
    </Modal>
  );
}