// Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React from 'react';
import { useToast } from '../stores/toastStore';

const ToastNotification: React.FC = () => {
    const { message, show, hideError } = useToast();

    const isError = message.startsWith("Error:");
    const isSuccess = message.startsWith("Success:");

    const bgClass = isError ? 'bg-danger' : isSuccess ? 'bg-success' : 'bg-info';
    const textColor = 'text-white'; // Adjust as desired
    const headerText = isError ? 'Error' : isSuccess ? 'Success' : 'Info';

    return (
        <div
            className="position-fixed mt3 start-50 translate-middle-x"
            style={{ zIndex: 2000, top: "3rem" }}
        >
            <div
                id="liveToast"
                className={`toast ${bgClass} ${textColor} ${show ? 'show' : ''}`}
                role="alert"
                aria-live="assertive"
                aria-atomic="true"
            >
                <div className="toast-header">
                    <strong className="me-auto">{headerText}</strong>
                    <button
                        type="button"
                        className="btn-close"
                        onClick={hideError}
                        aria-label="Close"
                    ></button>
                </div>
                <div className="toast-body">{message}</div>
            </div>
        </div>
    );
};

export default ToastNotification;
