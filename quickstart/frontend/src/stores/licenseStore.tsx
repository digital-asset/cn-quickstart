// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React, { createContext, useContext, useState, useCallback } from 'react';
import { useToast } from './toastStore';
import api from '../api';
import { generateCommandId } from '../utils/commandId';
import type {
    AuthenticatedUser,
    Client,
    License,
    LicenseRenewalRequest,
    LicenseRenewRequest,
    Metadata
} from "../openapi.d.ts";

interface LicenseState {
    licenses: License[];
    licenseRenewalRequests: LicenseRenewalRequest[];
}

interface LicenseContextType extends LicenseState {
    fetchUserInfo: () => Promise<void>;
    fetchLicenses: () => Promise<void>;
    fetchLicenseRenewalRequests: () => Promise<void>;
    renewLicense: (contractId: string, request: LicenseRenewRequest) => Promise<void>;
    expireLicense: (contractId: string, meta: Metadata) => Promise<void>;
    completeLicenseRenewal: (contractId: string) => Promise<void>;

    // Helper methods
    initiateLicenseRenewal: (contractId: string, description: string) => Promise<void>;
    initiateLicenseExpiration: (contractId: string, description: string) => Promise<void>;
}

const LicenseContext = createContext<LicenseContextType | undefined>(undefined);

export const LicenseProvider = ({ children }: { children: React.ReactNode }) => {
    const [licenses, setLicenses] = useState<License[]>([]);
    const [licenseRenewalRequests, setLicenseRenewalRequests] = useState<LicenseRenewalRequest[]>([]);
    const [, setUser] = useState<AuthenticatedUser | null>(null);
    const toast = useToast();

    const fetchUserInfo = useCallback(async () => {
        try {
            const client: Client = await api.getClient();
            const response = await client.getAuthenticatedUser();
            setUser(response.data);
        } catch (error) {
            toast.displayError('Error fetching user info');
        }
    }, [toast]);

    const fetchLicenses = useCallback(async () => {
        const client: Client = await api.getClient();
        const response = await client.listLicenses();
        setLicenses(response.data);
    }, []);

    const fetchLicenseRenewalRequests = useCallback(async () => {
        try {
            const client: Client = await api.getClient();
            const response = await client.listLicenseRenewalRequests();
            setLicenseRenewalRequests(response.data);
        } catch (error) {
            toast.displayError('Error fetching LicenseRenewalRequests');
        }
    }, [toast]);

    const renewLicense = useCallback(
        async (contractId: string, request: LicenseRenewRequest) => {
            try {
                const client: Client = await api.getClient();
                const commandId = generateCommandId();
                await client.renewLicense({ contractId, commandId }, request);
                // If renew succeeded, now try fetching licenses
                try {
                    await fetchLicenses();
                    toast.displaySuccess('License Renewal initiated successfully');
                } catch (e) {
                    toast.displayError('Error refreshing licenses after renewal');
                }
            } catch (error) {
                toast.displayError('Error renewing License');
            }
        },
        [toast, fetchLicenses]
    );

    const expireLicense = useCallback(
        async (contractId: string, meta: Metadata) => {
            try {
                const client: Client = await api.getClient();
                const commandId = generateCommandId();

                const response = await client.expireLicense(
                    { contractId, commandId },
                    { meta }
                );

                try {
                    await fetchLicenses();

                    toast.displaySuccess(response.data || 'License expired successfully');
                } catch (e) {
                    toast.displayError('Error refreshing licenses after expiration');
                }
            } catch (error: any) {
                const errorMessage = error?.response?.data || 'Error expiring License';
                toast.displayError(errorMessage);
            }
        },
        [toast, fetchLicenses]
    );


    const completeLicenseRenewal = useCallback(
        async (contractId: string) => {
            try {
                const client: Client = await api.getClient();
                const commandId = generateCommandId();
                await client.completeLicenseRenewal({ contractId, commandId });
                try {
                    await fetchLicenses();
                    await fetchLicenseRenewalRequests();
                    toast.displaySuccess('License renewal completed successfully');
                } catch (e) {
                    toast.displayError('Error refreshing data after completing renewal');
                }
            } catch (error: any) {
                if (error.response?.status === 404) {
                    toast.displayError('The license has not yet been paid for.');
                } else {
                    toast.displayError('Error completing License Renewal');
                }
            }
        },
        [toast, fetchLicenses, fetchLicenseRenewalRequests]
    );

    const initiateLicenseRenewal = useCallback(
        async (contractId: string, description: string) => {
            const request: LicenseRenewRequest = {
                licenseFeeCc: 100,
                licenseExtensionDuration: 'P30D',
                paymentAcceptanceDuration: 'P7D',
                description: description.trim(),
            };
            await renewLicense(contractId, request);
        },
        [renewLicense]
    );

    const initiateLicenseExpiration = useCallback(
        async (contractId: string, description: string) => {
            const meta = {
                data: {
                    description: description.trim(),
                },
            };
            await expireLicense(contractId, meta);
        },
        [expireLicense]
    );

    return (
        <LicenseContext.Provider
            value={{
                licenses,
                licenseRenewalRequests,
                fetchUserInfo,
                fetchLicenses,
                fetchLicenseRenewalRequests,
                renewLicense,
                expireLicense,
                completeLicenseRenewal,
                initiateLicenseRenewal,
                initiateLicenseExpiration,
            }}
        >
            {children}
        </LicenseContext.Provider>
    );
};

export const useLicenseStore = () => {
    const context = useContext(LicenseContext);
    if (context === undefined) {
        throw new Error('useLicenseStore must be used within a LicenseProvider');
    }
    return context;
};
