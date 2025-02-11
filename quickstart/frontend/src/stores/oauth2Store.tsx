// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React, { createContext, useContext, useState, useCallback } from 'react'
import { useToast } from './toastStore'
// @ts-ignore
import openApi from '../../../common/openapi.yaml'
import OpenAPIClientAxios from 'openapi-client-axios'

const api = new OpenAPIClientAxios({
    definition: openApi as any,
    withServer: { url: '/api' },
})

api.init()

interface OAuth2ClientRegistration {
    clientId: string
    clientSecret: string
    scope: string
    authorizationUri: string
    tokenUri: string
    jwkSetUri: string
    party: string
    preconfigured: boolean
    walletUrl: string
}

interface OAuth2State {
    registrations: OAuth2ClientRegistration[]
}

interface OAuth2ContextType extends OAuth2State {
    fetchRegistrations: () => Promise<void>
    createRegistration: (registration: OAuth2ClientRegistration) => Promise<void>
    deleteRegistration: (clientId: string) => Promise<void>
    setRegistrations: React.Dispatch<React.SetStateAction<OAuth2ClientRegistration[]>>
}

interface OAuth2ProviderProps {
    children: React.ReactNode
}

const OAuth2Context = createContext<OAuth2ContextType | undefined>(undefined)

export const OAuth2Provider = ({ children }: OAuth2ProviderProps) => {
    const [registrations, setRegistrations] = useState<OAuth2ClientRegistration[]>([])
    const toast = useToast()

    const fetchRegistrations = useCallback(async () => {
        try {
            const client = await api.getClient()
            const response = await client.listClientRegistrations()
            setRegistrations(response.data)
        } catch (error) {
            toast.displayError('Error fetching client registrations')
        }
    }, [toast])

    const createRegistration = useCallback(async (registration: OAuth2ClientRegistration) => {
        try {
            const client = await api.getClient()
            const response  = await client.createClientRegistration({}, registration)
            console.log(response)
            if (response.status !== 200) {
                throw new Error(`Unexpected response status: ${response.status}`)
            }
            setRegistrations(prev => [...prev, response.data])
        } catch (error) {
            toast.displayError('Error creating client registration')
        }
    }, [toast])

    const deleteRegistration = useCallback(async (clientId: string) => {
        try {
            const client = await api.getClient()
            await client.deleteClientRegistration({ clientId })
            setRegistrations(prev => prev.filter(reg => reg.clientId !== clientId))
        } catch (error) {
            toast.displayError('Error deleting client registration')
        }
    }, [toast])

    return (
        <OAuth2Context.Provider
            value={{
                registrations,
                setRegistrations,
                fetchRegistrations,
                createRegistration,
                deleteRegistration
            }}
        >
            {children}
        </OAuth2Context.Provider>
    )
}

export const useOAuth2Store = () => {
    const context = useContext(OAuth2Context)
    if (context === undefined) {
        throw new Error('useOAuth2Store must be used within an OAuth2Provider')
    }
    return context
}

export type { OAuth2ClientRegistration }
