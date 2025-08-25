import { useState } from 'react';
import Modal from './Modal.tsx';
import type { License, LicenseRenewalRequest, LicenseRenewRequest } from '../openapi';
import LicenseRenewModal from '../components/LicenseRenewModal.tsx';

type Props = {
  show: boolean;
  license: License | null;
  onClose: () => void;
  isAdmin: boolean;
  onIssueRenewal: (request: LicenseRenewRequest) => Promise<void> | void;
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
  const [showNewModal, setShowNewModal] = useState(false);

  function calculateStatus(renewal: LicenseRenewalRequest) {
    if (renewal.settleDeadlinePassed) {
      return 'EXPIRED';
    }
    if (!renewal.allocationCid) {
      return 'AWAITING_ACCEPTANCE';
    }
    return 'AWAITING_COMPLETION';
  }

  return (
    <Modal
      show={show}
      title={
        <div>License Renewal Requests</div>
      }
      onClose={onClose}
      backdrop="static"
      size="xl"
      zIndexBase={1500}
      dialogClassName="auto-width-modal"
      contentClassName="auto-width-content"
    >

      <LicenseRenewModal
        show={showNewModal && isAdmin}
        license={license}
        onIssueRenewal={(request) => {
          setShowNewModal(false);
          onIssueRenewal(request);
        }}
        onClose={() => {
          setShowNewModal(false);
        }}
      > 

      </LicenseRenewModal>

      <div><strong>License Contract ID:</strong> {license?.contractId.substring(0, 24)}...</div>     

      <br />
      {isAdmin && (
        <button
          className="btn btn-success btn-issue-renewal"
          onClick={() => setShowNewModal(true)}
        >
          New
        </button>
      )}

      <div className="renewals">
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
              <th style={{ width: '200px' }}>Description</th>
              <th style={{ width: '100px' }}>Status</th>
              {isAdmin && (
                <th style={{ width: '220px' }}>Actions</th>
              )}
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
                  <td className={`ellipsis-cell ${renewal.prepareDeadlinePassed && 'deadline-passed'}`}>
                    {formatDateTime(renewal.prepareUntil)}
                  </td>
                  <td className={`ellipsis-cell ${renewal.settleDeadlinePassed && 'deadline-passed'}`}>
                    {formatDateTime(renewal.settleBefore)}
                  </td>
                  <td className="ellipsis-cell">{renewal.description}</td>
                  <td className="ellipsis-cell">
                    {calculateStatus(renewal)}
                  </td>
                  {isAdmin && (
                    <td className="license-actions">
                      <>
                        {isAdmin && !renewal.settleDeadlinePassed && renewal.allocationCid && license && (
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
                          className="btn btn-danger btn-withdraw"
                          onClick={() => {
                            onWithdraw(renewal.contractId);
                          }}
                        >
                          Withdraw
                        </button>
                      )}
                    </td>
                  )}
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