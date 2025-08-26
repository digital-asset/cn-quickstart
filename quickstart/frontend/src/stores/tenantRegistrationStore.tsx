// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates.
// SPDX-License-Identifier: 0BSD

import React, { createContext, useContext, useState, useCallback } from 'react'
import { useToast } from './toastStore'
import api from "../api.ts";


export interface TenantRegistrationRequest {
    tenantId: string
    partyId: string
    walletUrl: string
    clientId?: string
    issuerUrl?: string
    users?: string[]
}

export interface TenantRegistration {
    tenantId: string
    partyId: string
    clientId?: string
    issuerUrl?: string
    walletUrl: string
    internal: boolean
    users?: string[]
}

interface TenantRegistrationState {
    registrations: TenantRegistration[]
}

interface TenantRegistrationContextType extends TenantRegistrationState {
    fetchTenantRegistrations: () => Promise<void>
    createTenantRegistration: (registration: TenantRegistrationRequest) => Promise<void>
    deleteTenantRegistration: (tenantId: string) => Promise<void>
    setRegistrations: React.Dispatch<React.SetStateAction<TenantRegistration[]>>
}

interface TenantRegistrationProviderProps {
    children: React.ReactNode
}

const TenantRegistrationContext = createContext<TenantRegistrationContextType | undefined>(
    undefined
)

export const TenantRegistrationProvider = ({
                                               children,
                                           }: TenantRegistrationProviderProps) => {
    const [registrations, setRegistrations] = useState<TenantRegistration[]>([])
    const toast = useToast()

    const fetchTenantRegistrations = useCallback(async () => {
        try {
            const client = await api.getClient()
            // New name: listTenantRegistrations
            const response = await client.listTenantRegistrations()
            setRegistrations(response.data)
        } catch (error) {
            toast.displayError('Error fetching tenant registrations')
        }
    }, [toast])

    const createTenantRegistration = useCallback(
        async (request: TenantRegistrationRequest) => {
            try {
                const client = await api.getClient()
                // New name: createTenantRegistration
                const response = await client.createTenantRegistration({}, request)
                const created = (response as any)?.data as TenantRegistration | undefined;
                if (created?.tenantId) {
                    setRegistrations(prev => {
                        if (prev.some(reg =>
                            reg.tenantId === created.tenantId ||
                            reg.clientId === created.clientId ||
                            reg.issuerUrl === created.issuerUrl)) return prev;
                        return [...prev, created];
                    });
                } else {
                    // No body? Fallback to a fresh list to keep UI in sync.
                    const list = await client.listTenantRegistrations();
                    setRegistrations(list.data);
                }
                toast.displaySuccess('Tenant registration created')
            } catch (error) {
                const { status, message } = extractError(error);
                if (status === 400) return toast.displayError(message ?? 'Invalid input');
                if (status === 409) return toast.displayError(message ?? 'Conflict: duplicate tenant/client/issuer');
                if (status === 500) return toast.displayError(message ?? 'Failed to create tenant registration');
                toast.displayError('Error creating tenant registration')
            }
        },
        [toast]
    )

    const deleteTenantRegistration = useCallback(async (tenantId: string) => {
        try {
            const client = await api.getClient()
            // New name: deleteTenantRegistration
            await client.deleteTenantRegistration({ tenantId: tenantId })
            setRegistrations((prev) => prev.filter((reg) => reg.tenantId !== tenantId))
            toast.displaySuccess('Tenant registration deleted')
        } catch (error) {
            toast.displayError('Error deleting tenant registration')
        }
    }, [toast])

    return (
        <TenantRegistrationContext.Provider
            value={{
                registrations,
                setRegistrations,
                fetchTenantRegistrations,
                createTenantRegistration,
                deleteTenantRegistration,
            }}
        >
            {children}
        </TenantRegistrationContext.Provider>
    )
}

export const useTenantRegistrationStore = () => {
    const context = useContext(TenantRegistrationContext)
    if (context === undefined) {
        throw new Error(
            'useTenantRegistrationStore must be used within a TenantRegistrationProvider'
        )
    }
    return context
}

function extractError(err: unknown): { status?: number; message?: string } {
    const anyErr = err as any;
    const status = anyErr?.response?.status ?? anyErr?.status;
    const message =
        anyErr?.response?.data?.message ?? // our ErrorResponse { error, message }
        anyErr?.message ??
        'Unexpected error';
    return { status, message };
}