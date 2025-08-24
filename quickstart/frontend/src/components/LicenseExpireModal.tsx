import { useState } from 'react';
import Modal from './Modal.tsx';
import type { License } from '../openapi';

type Props = {
  show: boolean;
  license: License | null;
  isAdmin: boolean;
  onClose: () => void;
  onExpire: (description: string) => Promise<void> | void;
};

export default function LicenseExpireModal({
  show,
  license,
  isAdmin,
  onClose,
  onExpire,
}: Props) {
  const [expireDescription, setExpireDescription] = useState('');

  return (
    <Modal
      show={show}
      title={
        <>
          <div><strong>License #:</strong> {license?.licenseNum}</div>
          <div><strong>Contract ID:</strong> {license?.contractId.substring(0, 24)}...</div>
          {isAdmin && (
            <div><strong>For User:</strong> {license?.user}</div>
          )}
        </>
      }
      onClose={() => {
        setExpireDescription('');
        onClose();
      }}
      backdrop="static"
      size="xl"
      zIndexBase={1500}
      dialogClassName="auto-width-modal"
      contentClassName="auto-width-content"
    >
      <div className="mb-4">
        <h6>Expire License</h6>
        <label>Description:</label>
        <input
          className="form-control mb-2 input-expire-description"
          placeholder='e.g. "License expired"'
          value={expireDescription}
          onChange={(e) => setExpireDescription(e.target.value)}
        />
        <button
          className="btn btn-danger btn-expire-license"
          onClick={async () => {
            if (!expireDescription.trim()) return;
            await onExpire(expireDescription);
            setExpireDescription('');
            onClose();
          }}
          disabled={!expireDescription.trim()}
        >
          Expire
        </button>
      </div>
    </Modal>
  );
}