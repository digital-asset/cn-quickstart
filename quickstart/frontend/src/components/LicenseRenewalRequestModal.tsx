import { useState } from 'react';
import Modal from './Modal.tsx';
import type { License, LicenseRenewalRequest, LicenseRenewRequest } from '../openapi';
import LicenseRenewModal from '../components/LicenseRenewModal.tsx';

type Props = {
  show: boolean;
  license: License | null;
  onClose: () => void;
  isAdmin: boolean;
  userWallet: string;
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
  userWallet,
  onIssueRenewal,
  onCompleteRenewal,
  onWithdraw,
  formatDateTime,
}: Props) {
  const [showNewModal, setShowNewModal] = useState(false);

  function makeStatus(renewal: LicenseRenewalRequest) {
    if (renewal.settleDeadlinePassed) {
      return (
        <span title="This renewal request has expired and may be withdrawn by the app-provider.">
          EXPIRED
        </span>
      );
    }
    if (!renewal.allocationCid) {
      return (
        <span title="Pending allocation acceptance in the app-user's wallet.">
          AWAITING_ACCEPTANCE
        </span>
      );
    }
    return (
      <span title="Pending completion by the app-provider.">
        AWAITING_COMPLETION
      </span>
    );
  }

  function handleIssueRenewal(request: LicenseRenewRequest) {
    setShowNewModal(false);
    onIssueRenewal(request);
  }

  function handleClose() {
    if (showNewModal) {
      setShowNewModal(false);
    } else {
      onClose();
    }
  }

  return (
    <Modal
      show={show}
      title={
        <div>License Renewal Requests</div>
      }
      onClose={handleClose}
      backdrop="static"
      size="xl"
      zIndexBase={1500}
      dialogClassName="auto-width-modal"
      contentClassName="auto-width-content"
    >

      <LicenseRenewModal
        show={showNewModal && isAdmin}
        license={license}
        onIssueRenewal={handleIssueRenewal}
        onClose={handleClose}
      /> 

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
              <th style={{ width: '100px' }}>Description</th>
              <th style={{ width: '150px', minWidth: '150px' }}>Status</th>
              <th style={{ width: '220px' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {license?.renewalRequests?.map((renewal) => {
              return (
                <tr key={renewal.contractId} className="renewal-row">
                  <td className="ellipsis-cell renewal-contract-id" data-testid="renewal-contract-id">{renewal.contractId}</td>
                  <td className="ellipsis-cell renewal-request-id" data-testid="renewal-request-id">{renewal.requestId}</td>
                  <td className="ellipsis-cell renewal-requested-at">{formatDateTime(renewal.requestedAt)}</td>
                  <td className="ellipsis-cell">{renewal.licenseExtensionDuration}</td>
                  <td className="ellipsis-cell">{renewal.licenseFeeAmount}</td>
                  <td className={`ellipsis-cell ${renewal.prepareDeadlinePassed && 'deadline-passed'}`}>
                    {formatDateTime(renewal.prepareUntil)}
                  </td>
                  <td className={`ellipsis-cell ${renewal.settleDeadlinePassed && 'deadline-passed'}`}>
                    {formatDateTime(renewal.settleBefore)}
                    {/* TODO https://github.com/digital-asset/cn-quickstart/issues/239
                         date component and remove server side computation of settleDeadlinePassed */}
                  </td>
                  <td className="ellipsis-cell">{renewal.description}</td>
                  <td className="ellipsis-cell">{makeStatus(renewal)}</td>
                  <td className="renewals-actions">
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
                    {!isAdmin && !renewal.settleDeadlinePassed && !renewal.allocationCid && (
                      <>Please accept the allocation request in your <a href={`${userWallet}/allocations`} target='_blank'>wallet</a>.</>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </Modal>
  );
}