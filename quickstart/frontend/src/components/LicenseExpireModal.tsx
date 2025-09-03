import { useState } from 'react';
import Modal from './Modal.tsx';
import type { License } from '../openapi';

type Props = {
  show: boolean;
  license: License | null;
  onClose: () => void;
  onArchive: (description: string) => Promise<void> | void;
};

export default function LicenseArchiveModal({
  show,
  license,
  onClose,
  onArchive,
}: Props) {
  const [expireDescription, setExpireDescription] = useState('');

  return (
    <Modal
      show={show}
      title={
        <div>Archive License</div>
      }
      onClose={() => {
        setExpireDescription('');
        onClose();
      }}
      onConfirm={async () => {
        if (!expireDescription.trim()) return;
        await onArchive(expireDescription);
        setExpireDescription('');
        onClose();
      }}   
      backdrop="static"
      size="lg"
      zIndexBase={1500}
      dialogClassName="xauto-width-modal"
      contentClassName="auto-width-content"
      confirmButtonClassName="btn-danger btn-expire-license"
      confirmButtonLabel='Archive'
    >
      <div className="mb-4">
        <div><strong>License Contract ID:</strong> {license?.contractId.substring(0, 24)}...</div>
        <br></br>
        <div className="d-flex align-items-center mb-2 flex-nowrap">
          <label htmlFor="expire-description" className="me-2 mb-0 flex-shrink-0">Description:</label>
          <input
            id="expire-description"
            className="form-control input-expire-description flex-grow-1"
            placeholder=''
            value={expireDescription}
            onChange={(e) => setExpireDescription(e.target.value)}
            style={{ minWidth: 0 }}
          />
        </div>
      </div>
    </Modal>
  );
}