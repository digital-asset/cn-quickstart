// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React, { createContext, useContext, useState, useCallback } from 'react';
import { useToast } from './toastStore';
import api from '../api';
import {AuthenticatedUser, ApiClient, toMeta, Contract} from '../types';
import { generateCommandId } from '../utils/commandId';
import {AppInstallRequest} from "../../generated/quickstart_licensing/Licensing/AppInstall";

interface AppInstallRequestState {
    appInstallRequests: Contract<AppInstallRequest>[];
}

interface AppInstallRequestContextType extends AppInstallRequestState {
    fetchUserInfo: () => Promise<void>;
    fetchAppInstallRequests: () => Promise<void>;
    acceptAppInstallRequest: (contractId: string, installMeta: Record<string, any>, meta: Record<string, any>) => Promise<void>;
    rejectAppInstallRequest: (contractId: string, meta: Record<string, any>) => Promise<void>;
    cancelAppInstallRequest: (contractId: string, meta: Record<string, any>) => Promise<void>;
}

const AppInstallRequestContext = createContext<AppInstallRequestContextType | undefined>(undefined);

export const AppInstallRequestProvider = ({ children }: { children: React.ReactNode }) => {
    const [appInstallRequests, setAppInstallRequests] = useState<Contract<AppInstallRequest>[]>([]);
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

    const fetchAppInstallRequests = useCallback(async () => {
        try {
            const client: ApiClient = await api.getClient();
            const response = await client.listAppInstallRequests();
            setAppInstallRequests(response.data);
        } catch (error) {
            toast.displayError('Error fetching AppInstallRequests');
        }
    }, [toast]);

    const acceptAppInstallRequest = useCallback(
        async (contractId: string, installMeta: Record<string, any>, meta: Record<string, any>) => {
            try {
                const client: ApiClient = await api.getClient();
                const commandId = generateCommandId();
                await client.acceptAppInstallRequest({ contractId, commandId }, { installMeta: installMeta.toMeta, meta: meta.toMeta });
                await fetchAppInstallRequests();
            } catch (error) {
                toast.displayError('Error accepting AppInstallRequest');
            }
        },
        [toast, fetchAppInstallRequests]
    );

    const rejectAppInstallRequest = useCallback(
        async (contractId: string, meta: Record<string, any>) => {
            try {
                const client: ApiClient = await api.getClient();
                const commandId = generateCommandId();
                await client.rejectAppInstallRequest({ contractId, commandId }, { meta: meta.toMeta });
                await fetchAppInstallRequests();
            } catch (error) {
                toast.displayError('Error rejecting AppInstallRequest');
            }
        },
        [toast, fetchAppInstallRequests]
    );

    const cancelAppInstallRequest = useCallback(
        async (contractId: string, meta: Record<string, any>) => {
            try {
                const client: ApiClient = await api.getClient();
                const commandId = generateCommandId();
                await client.cancelAppInstallRequest({ contractId, commandId }, { meta: toMeta(meta) });
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
