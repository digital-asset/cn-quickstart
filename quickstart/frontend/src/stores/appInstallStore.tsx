// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React, { createContext, useContext, useState, useCallback } from 'react';
import api from '../api';
import { generateCommandId } from '../utils/commandId';
import { useToast } from './toastStore';
import type {
    AppInstall as ApiAppInstall,
    AppInstallRequest as ApiAppInstallRequest,
    AppInstallRequestAccept,
    AppInstallRequestReject,
    AppInstallCreateLicenseRequest,
    AppInstallCreateLicenseResult,
    AppInstallCancel,
    Client,
    Metadata,
} from '../openapi.d.ts';
import { AppInstallUnified } from '../types';

interface AppInstallState {
    unifiedInstalls: AppInstallUnified[];
}

interface AppInstallContextType extends AppInstallState {
    fetchAll: () => Promise<void>;
    accept: (contractId: string, installMeta: Metadata, meta: Metadata) => Promise<void>;
    reject: (contractId: string, meta: Metadata) => Promise<void>;
    cancelInstall: (contractId: string, meta: Metadata) => Promise<void>;
    createLicense: (contractId: string, meta: Metadata) => Promise<AppInstallCreateLicenseResult | undefined>;
}

const AppInstallContext = createContext<AppInstallContextType | undefined>(undefined);

export const AppInstallProvider = ({ children }: { children: React.ReactNode }) => {
    const [unifiedInstalls, setUnifiedInstalls] = useState<AppInstallUnified[]>([]);
    const toast = useToast();

    const fetchAll = useCallback(async () => {
        try {
            const client: Client = await api.getClient();
            const requestsResponse = await client.listAppInstallRequests();
            const requests: ApiAppInstallRequest[] = requestsResponse.data;
            const installsResponse = await client.listAppInstalls();
            const installs: ApiAppInstall[] = installsResponse.data as ApiAppInstall[];

            const unifiedRequests: AppInstallUnified[] = requests.map((r) => ({
                status: 'REQUEST',
                contractId: r.contractId,
                provider: r.provider,
                user: r.user,
                meta: r.meta,
                numLicensesCreated: 0,
            }));
            const unifiedInstallRecords: AppInstallUnified[] = installs.map((i) => ({
                status: 'INSTALL', 
                contractId: i.contractId,
                provider: i.provider,
                user: i.user,
                meta: i.meta,
                numLicensesCreated: i.numLicensesCreated || 0,
            }));

            setUnifiedInstalls([...unifiedRequests, ...unifiedInstallRecords]);
        } catch (error) {
            toast.displayError('Error fetching AppInstall data');
        }
    }, [toast]);

    const accept = useCallback(
        async (contractId: string, installMeta: Metadata, meta: Metadata) => {
            try {
                const client: Client = await api.getClient();
                const commandId = generateCommandId();
                await client.acceptAppInstallRequest(
                    { contractId, commandId },
                    { installMeta, meta } as AppInstallRequestAccept
                );
                await fetchAll();
                toast.displaySuccess(`Accepted AppInstallRequest ${contractId}`);
            } catch (error) {
                toast.displayError('Error accepting AppInstallRequest');
            }
        },
        [toast, fetchAll]
    );

    const reject = useCallback(
        async (contractId: string, meta: Metadata) => {
            try {
                const client: Client = await api.getClient();
                const commandId = generateCommandId();
                await client.rejectAppInstallRequest(
                    { contractId, commandId },
                    { meta } as AppInstallRequestReject
                );
                await fetchAll();
                toast.displaySuccess(`Rejected AppInstallRequest ${contractId}`);
            } catch (error) {
                toast.displayError('Error rejecting AppInstallRequest');
            }
        },
        [toast, fetchAll]
    );

    const cancelInstall = useCallback(
        async (contractId: string, meta: Metadata) => {
            try {
                const client: Client = await api.getClient();
                const commandId = generateCommandId();
                await client.cancelAppInstall(
                    { contractId, commandId },
                    { meta } as AppInstallCancel
                );
                await fetchAll();
                toast.displaySuccess(`Canceled AppInstall ${contractId}`);
            } catch (error) {
                toast.displayError('Error canceling AppInstall');
            }
        },
        [toast, fetchAll]
    );

    const createLicense = useCallback(
        async (contractId: string, meta: Metadata) => {
            try {
                const client: Client = await api.getClient();
                const body: AppInstallCreateLicenseRequest = { params: { meta } };
                const commandId = generateCommandId();
                const response = await client.createLicense({ contractId, commandId }, body);
                await fetchAll();
                toast.displaySuccess(`Created License: ${response.data?.licenseId}`);
                return response.data;
            } catch (error) {
                toast.displayError('Error creating License from AppInstall');
            }
        },
        [toast, fetchAll]
    );

    return (
        <AppInstallContext.Provider
            value={{
                unifiedInstalls,
                fetchAll,
                accept,
                reject,
                cancelInstall,
                createLicense,
            }}
        >
            {children}
        </AppInstallContext.Provider>
    );
};

export const useAppInstallStore = () => {
    const context = useContext(AppInstallContext);
    if (context === undefined) {
        throw new Error('useAppInstallStore must be used within an AppInstallProvider');
    }
    return context;
};
