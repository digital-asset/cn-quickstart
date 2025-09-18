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
import { withErrorHandling } from "../utils/error";

interface AppInstallState {
    unifiedInstalls: AppInstallUnified[];
}

interface AppInstallContextType extends AppInstallState {
    fetchAll: () => Promise<void>;
    accept: (contractId: string, installMeta: Metadata, meta: Metadata) => Promise<void>;
    reject: (contractId: string, meta: Metadata) => Promise<void>;
    cancelInstall: (contractId: string, meta: Metadata) => Promise<void>;
    createLicense: (contractId: string, meta: Metadata) => Promise<AppInstallCreateLicenseResult | void>;
}

const AppInstallContext = createContext<AppInstallContextType | undefined>(undefined);

export const AppInstallProvider = ({ children }: { children: React.ReactNode }) => {
    const [unifiedInstalls, setUnifiedInstalls] = useState<AppInstallUnified[]>([]);
    const toast = useToast();

    const fetchAll = useCallback(
        withErrorHandling(`Fetching AppInstall data`)(async () => {
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
    }), [withErrorHandling, setUnifiedInstalls, toast]);

    const accept = useCallback(
        withErrorHandling(`Accepting AppInstallRequest`)(async (contractId: string, installMeta: Metadata, meta: Metadata) => {
            const client: Client = await api.getClient();
            const commandId = generateCommandId();
            await client.acceptAppInstallRequest(
                { contractId, commandId },
                { installMeta, meta } as AppInstallRequestAccept
            );
            await fetchAll();
            toast.displaySuccess(`Accepted AppInstallRequest ${contractId}`);
        }),
        [withErrorHandling, toast, fetchAll]
    );

    const reject = useCallback(
        withErrorHandling(`Rejecting AppInstallRequest`)(async (contractId: string, meta: Metadata) => {
            const client: Client = await api.getClient();
            const commandId = generateCommandId();
            await client.rejectAppInstallRequest(
                { contractId, commandId },
                { meta } as AppInstallRequestReject
            );
            await fetchAll();
            toast.displaySuccess(`Rejected AppInstallRequest ${contractId}`);
        }),
        [withErrorHandling, toast, fetchAll]
    );

    const cancelInstall = useCallback(
        withErrorHandling(`Canceling AppInstall`)(async (contractId: string, meta: Metadata) => {
            const client: Client = await api.getClient();
            const commandId = generateCommandId();
            await client.cancelAppInstall(
                { contractId, commandId },
                { meta } as AppInstallCancel
            );
            await fetchAll();
            toast.displaySuccess(`Canceled AppInstall ${contractId}`);
        }),
        [withErrorHandling, toast, fetchAll]
    );

    const createLicense = useCallback(
        withErrorHandling(`Creating License from AppInstall`)(async (contractId: string, meta: Metadata) => {
            const client: Client = await api.getClient();
            const body: AppInstallCreateLicenseRequest = { params: { meta } };
            const commandId = generateCommandId();
            const response = await client.createLicense({ contractId, commandId }, body);
            await fetchAll();
            toast.displaySuccess(`Created License: ${response.data?.licenseId}`);
            return response.data;
        }),
        [withErrorHandling, toast, fetchAll]
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
