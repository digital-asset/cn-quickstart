import { useState } from 'react';
import Modal from './Modal.tsx';
import DurationInput from './DurationInput';
import type { License, LicenseRenewRequest } from '../openapi';
import { formatDateTime } from '../utils/format';
import { toISO_8601 } from '../utils/duration';


type RenewData = {
  extension: string;
  feeAmount: number;
  prepareIn: string;
  settleIn: string;
  description: string;
};

const defaultData: RenewData = {
  extension: "30d",
  feeAmount: 100,
  prepareIn: '10m',
  settleIn: '20m',
  description: '',
};

type Props = {
  show: boolean;
  license: License | null;
  onClose: () => void;
  onIssueRenewal: (request: LicenseRenewRequest) => Promise<void> | void;
};

export default function LicenseRenewModal({
  show,
  license,
  onClose,
  onIssueRenewal,
}: Props) {
  const [renewData, setRenewData] = useState(defaultData);  

  return (
    <Modal
        show={show}
        title="Renew License"
        confirmButtonLabel="Issue License Renewal Request"
        confirmButtonClassName='btn-success'
        confirmButtonDisabled={!renewData.description.trim()}
        onClose={onClose}
        onConfirm={async () => {
          if (!renewData.description.trim() && !renewData.prepareIn.trim() && !renewData.settleIn.trim() && !renewData.extension) return;
          const request: LicenseRenewRequest = {
              licenseExtensionDuration: toISO_8601(renewData.extension),
              licenseFeeCc: renewData.feeAmount,
              prepareUntilDuration: toISO_8601(renewData.prepareIn),
              settleBeforeDuration: toISO_8601(renewData.settleIn),
              description: renewData.description.trim()
          };
          onIssueRenewal(request);
        }}
        backdrop="static"
        size="xl"
        zIndexBase={2000}
      >
          <div><strong>License Contract ID:</strong> {license?.contractId.substring(0, 24)}...</div>     
          <div className={`${license?.isExpired && 'deadline-passed'}`}><strong>Expires at:</strong> {formatDateTime(license?.expiresAt)}</div>     

          <br />

          <div className="mb-4">
            {(() => {
              const update = (partial: Partial<RenewData>) =>
                setRenewData({ ...renewData, ...(partial as RenewData) });

              return (
                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: '160px 1fr',
                    columnGap: '12px',
                    rowGap: '8px',
                    alignItems: 'center',
                  }}
                >
                  <label className="text-end pe-2 mb-0">Extension:</label>
                  <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                   <DurationInput
                      units="dhm"
                      value={renewData.extension}
                      onChange={(v) => {
                        update({ extension: v });
                      }}
                    />
                  </div>

                  <label className="text-end pe-2 mb-0">Fee:</label>
                  <div>
                    <input
                      type="number"
                      min={0}
                      className="form-control"
                      value={renewData.feeAmount}
                      onChange={(e) => update({ feeAmount: Number(e.target.value) || 0 })}
                      style={{ width: '120px' }}
                    />
                  </div>

                  <label className="text-end pe-2 mb-0">Prepare in:</label>
                  <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                    <DurationInput
                      units="smhd"
                      value={renewData.prepareIn}
                      onChange={(v) => {
                        update({ prepareIn: v });
                      }}
                    />
                  </div>

                  <label className="text-end pe-2 mb-0">Settle in:</label>
                  <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                    <DurationInput
                      units="smhd"
                      value={renewData.settleIn}
                      onChange={(v) => {
                        update({ settleIn: v });
                      }}
                    />
                  </div>

                  <label className="text-end pe-2 mb-0">Description:</label>
                  <div>
                    <input
                      className="form-control mb-2 input-renew-description"
                      placeholder=''
                      value={renewData.description}
                      onChange={(e) => update({ description: e.target.value })}
                    />
                  </div>
                </div>
              );
            })()}
          </div>
      </Modal>
  );
}