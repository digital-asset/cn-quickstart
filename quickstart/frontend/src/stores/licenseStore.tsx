// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React, { createContext, useContext, useState, useCallback } from 'react';
import { useToast } from './toastStore';
import api from '../api';
import {ApiClient, AuthenticatedUser, Contract, LicenseRenewalRequestComplete, toRelTime, toNumeric} from '../types';
import { generateCommandId } from '../utils/commandId';
import {License, LicenseRenewalRequest} from "../../generated/quickstart_licensing/Licensing/License";
import License_Renew = License.License_Renew;

interface LicenseState {
    licenses: Contract<License>[];
    licenseRenewalRequests: Contract<LicenseRenewalRequest>[];
}

interface LicenseContextType extends LicenseState {
    fetchUserInfo: () => Promise<void>;
    fetchLicenses: () => Promise<void>;
    fetchLicenseRenewalRequests: () => Promise<void>;
    renewLicense: (contractId: string, request: License_Renew) => Promise<void>;
    expireLicense: (contractId: string, meta: Record<string, any>) => Promise<void>;
    completeLicenseRenewal: (contractId: string, request: LicenseRenewalRequestComplete) => Promise<void>;

    // Helper methods
    initiateLicenseRenewal: (contractId: string, description: string) => Promise<void>;
    initiateLicenseExpiration: (contractId: string, description: string) => Promise<void>;
}

const LicenseContext = createContext<LicenseContextType | undefined>(undefined);

export const LicenseProvider = ({ children }: { children: React.ReactNode }) => {
    const [licenses, setLicenses] = useState<Contract<License>[]>([]);
    const [licenseRenewalRequests, setLicenseRenewalRequests] = useState<Contract<LicenseRenewalRequest>[]>([]);
    const [, setUser] = useState<AuthenticatedUser | null>(null);
    const toast = useToast();

    const fetchUserInfo = useCallback(async () => {
        try {
            const client: ApiClient = await api.getClient();
            const response = await client.getAuthenticatedUser();
            setUser(response.data);
        } catch (error) {
            toast.displayError('Error fetching user info');
        }
    }, [toast]);

    const fetchLicenses = useCallback(async () => {
        const client: ApiClient = await api.getClient();
        const response = await client.listLicenses();
        setLicenses(response.data);
    }, []);

    const fetchLicenseRenewalRequests = useCallback(async () => {
        try {
            const client: ApiClient = await api.getClient();
            const response = await client.listLicenseRenewalRequests();
            setLicenseRenewalRequests(response.data);
        } catch (error) {
            toast.displayError('Error fetching LicenseRenewalRequests');
        }
    }, [toast]);

    const renewLicense = useCallback(
        async (contractId: string, request: License_Renew) => {
            try {
                const client: ApiClient = await api.getClient();
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
        async (contractId: string, meta: Record<string, any>) => {
            try {
                const client: ApiClient = await api.getClient();
                const commandId = generateCommandId();

                const serverMessage = await client.expireLicense(
                    { contractId, commandId },
                    {
                        actor: "???",
                        meta: meta.toMeta
                    }
                );

                try {
                    await fetchLicenses();

                    toast.displaySuccess(serverMessage || 'License expired successfully');
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
        async (contractId: string, request: LicenseRenewalRequestComplete) => {
            try {
                const client: ApiClient = await api.getClient();
                const commandId = generateCommandId();
                await client.completeLicenseRenewal({ contractId, commandId }, request);
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
            const request: License_Renew = {
                licenseFeeCc: toNumeric(100),
                licenseExtensionDuration: toRelTime('P30D'),
                paymentAcceptanceDuration: toRelTime('P7D'),
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
