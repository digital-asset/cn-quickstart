// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React, { createContext, useContext, useState, useCallback } from 'react';
import { useToast } from './toastStore';
import api from '../api';
import { AuthenticatedUser, ApiClient, AppInstall, AppInstallCancel, AppInstallCreateLicenseRequest, AppInstallCreateLicenseResult } from '../types';
import { generateCommandId } from '../utils/commandId';

interface AppInstallState {
    appInstalls: AppInstall[];
}

interface AppInstallContextType extends AppInstallState {
    fetchUserInfo: () => Promise<void>;
    fetchAppInstalls: () => Promise<void>;
    cancelAppInstall: (contractId: string, meta: Record<string, any>) => Promise<void>;
    createLicenseFromAppInstall: (contractId: string, params: Record<string, any>) => Promise<AppInstallCreateLicenseResult | undefined>;
}

const AppInstallContext = createContext<AppInstallContextType | undefined>(undefined);

export const AppInstallProvider = ({ children }: { children: React.ReactNode }) => {
    const [appInstalls, setAppInstalls] = useState<AppInstall[]>([]);
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

    const fetchAppInstalls = useCallback(async () => {
        try {
            const client: ApiClient = await api.getClient();
            const response = await client.listAppInstalls();
            setAppInstalls(response.data);
        } catch (error) {
            toast.displayError('Error fetching AppInstalls');
        }
    }, [toast]);

    const cancelAppInstall = useCallback(
        async (contractId: string, meta: Record<string, any>) => {
            try {
                const client: ApiClient = await api.getClient();
                const body: AppInstallCancel = {
                    meta: { data: meta }
                };
                const commandId = generateCommandId();
                await client.cancelAppInstall({ contractId, commandId }, body);
                await fetchAppInstalls();
            } catch (error) {
                toast.displayError('Error canceling AppInstall');
            }
        },
        [toast, fetchAppInstalls]
    );

    const createLicenseFromAppInstall = useCallback(
        async (contractId: string, params: Record<string, any>) => {
            try {
                const client: ApiClient = await api.getClient();
                const body: AppInstallCreateLicenseRequest = {
                    params: { meta: { data: params } }
                };
                const commandId = generateCommandId();
                const response = await client.createLicense({ contractId, commandId }, body);
                // Refresh the list after creation if needed
                await fetchAppInstalls();
                return response.data;
            } catch (error) {
                toast.displayError('Error creating License from AppInstall');
            }
        },
        [toast, fetchAppInstalls]
    );

    return (
        <AppInstallContext.Provider
            value={{
                appInstalls,
                fetchUserInfo,
                fetchAppInstalls,
                cancelAppInstall,
                createLicenseFromAppInstall,
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
