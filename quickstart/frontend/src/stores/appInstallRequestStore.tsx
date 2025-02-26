// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React, { createContext, useContext, useState, useCallback } from 'react';
import { useToast } from './toastStore';
import api from '../api';
import { generateCommandId } from '../utils/commandId';
import type {AppInstallRequest, AuthenticatedUser, Client, Metadata} from "../openapi.d.ts";

interface AppInstallRequestState {
    appInstallRequests: AppInstallRequest[];
}

interface AppInstallRequestContextType extends AppInstallRequestState {
    fetchUserInfo: () => Promise<void>;
    fetchAppInstallRequests: () => Promise<void>;
    acceptAppInstallRequest: (contractId: string, installMeta: Metadata, meta: Metadata) => Promise<void>;
    rejectAppInstallRequest: (contractId: string, meta: Metadata) => Promise<void>;
    cancelAppInstallRequest: (contractId: string, meta: Metadata) => Promise<void>;
}

const AppInstallRequestContext = createContext<AppInstallRequestContextType | undefined>(undefined);

export const AppInstallRequestProvider = ({ children }: { children: React.ReactNode }) => {
    const [appInstallRequests, setAppInstallRequests] = useState<AppInstallRequest[]>([]);
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

    const fetchAppInstallRequests = useCallback(async () => {
        try {
            const client: Client = await api.getClient();
            const response = await client.listAppInstallRequests();
            setAppInstallRequests(response.data);
        } catch (error) {
            toast.displayError('Error fetching AppInstallRequests');
        }
    }, [toast]);

    const acceptAppInstallRequest = useCallback(
        async (contractId: string, installMeta: Metadata, meta: Metadata) => {
            try {
                const client: Client = await api.getClient();
                const commandId = generateCommandId();
                await client.acceptAppInstallRequest({ contractId, commandId }, { installMeta, meta });
                await fetchAppInstallRequests();
            } catch (error) {
                toast.displayError('Error accepting AppInstallRequest');
            }
        },
        [toast, fetchAppInstallRequests]
    );

    const rejectAppInstallRequest = useCallback(
        async (contractId: string, meta: Metadata) => {
            try {
                const client: Client = await api.getClient();
                const commandId = generateCommandId();
                await client.rejectAppInstallRequest({ contractId, commandId }, { meta });
                await fetchAppInstallRequests();
            } catch (error) {
                toast.displayError('Error rejecting AppInstallRequest');
            }
        },
        [toast, fetchAppInstallRequests]
    );

    const cancelAppInstallRequest = useCallback(
        async (contractId: string, meta: Metadata) => {
            try {
                const client: Client = await api.getClient();
                const commandId = generateCommandId();
                await client.cancelAppInstallRequest({ contractId, commandId }, { meta });
                await fetchAppInstallRequests();
            } catch (error) {
                toast.displayError('Error canceling AppInstallRequest');
            }
        },
        [toast, fetchAppInstallRequests]
    );

    return (
        <AppInstallRequestContext.Provider
            value={{
                appInstallRequests,
                fetchUserInfo,
                fetchAppInstallRequests,
                acceptAppInstallRequest,
                rejectAppInstallRequest,
                cancelAppInstallRequest,
            }}
        >
            {children}
        </AppInstallRequestContext.Provider>
    );
};

export const useAppInstallRequestStore = () => {
    const context = useContext(AppInstallRequestContext);
    if (context === undefined) {
        throw new Error('useAppInstallRequestStore must be used within an AppInstallRequestProvider');
    }
    return context;
};
