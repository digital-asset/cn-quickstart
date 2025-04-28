// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates.
// SPDX-License-Identifier: 0BSD

import React, { createContext, useContext, useState, useCallback } from 'react'
import { useToast } from './toastStore'
// @ts-ignore
import openApi from '../../../common/openapi.yaml'
import api from "../api.ts";


export interface TenantRegistration {
    tenantId: string
    partyId: string
    clientId: string
    issuerUrl: string
    walletUrl: string
    internal: boolean
    users: string[]
}

interface TenantRegistrationState {
    registrations: TenantRegistration[]
}

interface TenantRegistrationContextType extends TenantRegistrationState {
    fetchTenantRegistrations: () => Promise<void>
    createTenantRegistration: (registration: TenantRegistration) => Promise<void>
    deleteTenantRegistration: (clientId: string) => Promise<void>
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
        async (registration: TenantRegistration) => {
            try {
                const client = await api.getClient()
                // New name: createTenantRegistration
                const response = await client.createTenantRegistration({}, registration)
                if (response.status !== 200) {
                    throw new Error(`Unexpected response status: ${response.status}`)
                }
                setRegistrations((prev) => [...prev, response.data])
            } catch (error) {
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
