// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React, { createContext, useContext, useState, useCallback } from 'react';
import { useToast } from './toastStore';
import api from '../api';
import { generateCommandId } from '../utils/commandId';
import type {
  Client, CompleteLicenseRenewalRequest,
  License,
  LicenseRenewRequest,
  Metadata,
} from '../openapi.d.ts';

/**
 * The core shape of the License-related application state.
 */
interface LicenseState {
    licenses: License[];
}

/**
 * Methods for retrieving and modifying License data throughout the application.
 */
interface LicenseContextType extends LicenseState {
    fetchLicenses: () => Promise<void>;
    renewLicense: (contractId: string, request: LicenseRenewRequest) => Promise<void>;
    expireLicense: (contractId: string, meta: Metadata) => Promise<void>;
    completeLicenseRenewal: (contractId: string, renewalRequestContractId: string, allocationContractId: string) => Promise<string | undefined>;
    initiateLicenseRenewal: (contractId: string, request: LicenseRenewRequest) => Promise<void>;
    initiateLicenseExpiration: (contractId: string, description: string) => Promise<void>;
    withdrawLicenseRenewalRequest: (contractId: string) => Promise<void>;
}

const LicenseContext = createContext<LicenseContextType | undefined>(undefined);

/**
 * Provides shared License state and actions to manage Licenses and their renewals.
 */
export const LicenseProvider = ({ children }: { children: React.ReactNode }) => {
    const [licenses, setLicenses] = useState<License[]>([]);
    const toast = useToast();

    /**
     * Fetches all Licenses from the backend, including any associated renewal requests.
     */
    const fetchLicenses = useCallback(async () => {
        try {
            const client: Client = await api.getClient();
            const response = await client.listLicenses();
            setLicenses(response.data);
        } catch (error) {
            toast.displayError('Error fetching Licenses');
        }
    }, [toast]);

    /**
     * Sends a request to renew a specific License, optionally refreshing the License list on success.
     */
    const renewLicense = useCallback(
        async (contractId: string, request: LicenseRenewRequest) => {
            try {
                const client: Client = await api.getClient();
                const commandId = generateCommandId();
                await client.renewLicense({ contractId, commandId }, request);
                await fetchLicenses();
                toast.displaySuccess('License Renewal initiated successfully');
            } catch (error) {
                toast.displayError('Error renewing License');
            }
        },
        [fetchLicenses, toast]
    );

    /**
     * Sends a request to expire a specific License, optionally refreshing the License list on success.
     */
    const expireLicense = useCallback(
        async (contractId: string, meta: Metadata) => {
            try {
                const client: Client = await api.getClient();
                const commandId = generateCommandId();
                const response = await client.expireLicense({ contractId, commandId }, { meta });
                await fetchLicenses();
                toast.displaySuccess(response.data || 'License expired successfully');
            } catch (error: any) {
                const errorMessage = error?.response?.data || 'Error expiring License';
                toast.displayError(errorMessage);
            }
        },
        [fetchLicenses, toast]
    );

    /**
     * Sends a request to reject a specific License renewal request.
     */
    const withdrawLicenseRenewalRequest = useCallback(
        async (contractId: string) => {
            try {
                const client: Client = await api.getClient();
                const commandId = generateCommandId();
                await client.withdrawLicenseRenewalRequest({ contractId, commandId });
                await fetchLicenses();
                toast.displaySuccess('License renewal request withdrawn successfully');
            } catch (error: any) {
                const errorMessage = error?.response?.data || 'Error withdrawing License renewal request';
                toast.displayError(errorMessage);
            }
        },
        [fetchLicenses, toast]
    );

    /**
     * Completes the renewal flow after the renewal request has been paid.
     */
    const completeLicenseRenewal = useCallback(
        async (contractId: string, renewalRequestContractId: string, allocationContractId: string) => {
            try {
                const client: Client = await api.getClient();
                const commandId = generateCommandId();

                const request: CompleteLicenseRenewalRequest = {
                  renewalRequestContractId: renewalRequestContractId,
                  allocationContractId: allocationContractId
                };

                const result = await client.completeLicenseRenewal({ contractId, commandId }, request);
                await fetchLicenses();
                toast.displaySuccess('License renewal completed successfully');
                return result.data.licenseId;
            } catch (error: any) {
                if (error.response?.status === 404) {
                    toast.displayError('The license has not yet been paid for.');
                } else {
                    toast.displayError('Error completing License Renewal');
                }
            }
        },
        [fetchLicenses, toast]
    );

    /**
     * Helper to initiate a new License renewal with fixed parameters.
     */
    const initiateLicenseRenewal = useCallback(
        async (contractId: string, request: LicenseRenewRequest) => {
            await renewLicense(contractId, request);
        },
        [renewLicense]
    );

    /**
     * Helper to begin the License expiration process with a basic description.
     */
    const initiateLicenseExpiration = useCallback(
        async (contractId: string, description: string) => {
            const meta = {
                data: { description: description.trim() },
            };
            await expireLicense(contractId, meta);
        },
        [expireLicense]
    );

    return (
        <LicenseContext.Provider
            value={{
                licenses,
                fetchLicenses,
                renewLicense,
                expireLicense,
                completeLicenseRenewal,
                initiateLicenseRenewal,
                initiateLicenseExpiration,
                withdrawLicenseRenewalRequest
            }}
        >
            {children}
        </LicenseContext.Provider>
    );
};

/**
 * Hook for accessing License context within React components.
 */
export const useLicenseStore = () => {
    const context = useContext(LicenseContext);
    if (context === undefined) {
        throw new Error('useLicenseStore must be used within a LicenseProvider');
    }
    return context;
};
