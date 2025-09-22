// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates.
// SPDX-License-Identifier: 0BSD

import React, { useEffect, useState } from 'react'
import { useTenantRegistrationStore } from '../stores/tenantRegistrationStore'
import type { TenantRegistrationRequest } from "../openapi.d.ts"
import {useToast} from '../stores/toastStore'
import api from '../api'
import { Client, FeatureFlags } from "../openapi"

const TenantRegistrationView: React.FC = () => {
    const {
        registrations,
        fetchTenantRegistrations,
        createTenantRegistration,
        deleteTenantRegistration,
    } = useTenantRegistrationStore()

    const toast = useToast()
    const [featureFlags, setFeatureFlags] = useState<FeatureFlags | null>(null)

    const [formData, setFormData] = useState<TenantRegistrationRequest>({
        tenantId: '',
        partyId: '',
        clientId: '',
        issuerUrl: '',
        walletUrl: '',
        users: []
    })

    const fetchFeatureFlags = async () => {
        try {
            const client: Client = await api.getClient()
            const response = await client.getFeatureFlags()
            setFeatureFlags(response.data)
        } catch {
            toast.displayError('Error fetching feature flags')
        }
    }

    useEffect(() => {
        fetchFeatureFlags()
        fetchTenantRegistrations()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    // Prefill Party ID (unchangeable)
    useEffect(() => {
        const candidate = registrations[0]?.partyId?.trim()
        if (candidate && formData.partyId !== candidate) {
            setFormData(prev => ({ ...prev, partyId: candidate }))
        }
    }, [registrations, formData.partyId])

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target
        setFormData(prev => ({
            ...prev,
            [name]: name === 'users' ? value.split(',').map(u => u.trim()).filter(Boolean) : value,
        }))
    }

    // Validation driven by feature flags
    const validate = (): string | null => {
        const t = formData.tenantId.trim()
        const p = formData.partyId.trim()
        if (!t) return 'Tenant ID is required'
        if (!p) return 'Party ID is required'

        if (featureFlags?.authMode === 'oauth2') {
            if (!formData.clientId?.trim()) return 'Client ID is required (OAuth2)'
            if (!formData.issuerUrl?.trim()) return 'Issuer URL is required (OAuth2)'
        }

        if (featureFlags?.authMode === 'shared-secret') {
            if (!formData.users || formData.users.length === 0) {
                return 'At least one user is required (Shared Secret)'
            }
        }
        return null
    }

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        const error = validate()
        if (error) {
            toast.displayError(error)
            return
        }
        await createTenantRegistration(formData)

        // Reset the form but keep the prefilled (unchangeable) partyId
        setFormData(prev => ({
            tenantId: '',
            partyId: prev.partyId,
            clientId: '',
            issuerUrl: '',
            walletUrl: '',
            users: [],
        }))
    }

    const handleDelete = async (tenantId: string) => {
        if (window.confirm('Are you sure you want to delete this tenant registration?')) {
            await deleteTenantRegistration(tenantId)
        }
    }

    const isSubmittingBlocked = !formData.partyId.trim()

    return (
        <div>
            <form onSubmit={handleSubmit}>
                <div className="mb-3">
                    <label htmlFor="tenantId" className="form-label">Tenant ID:</label>
                    <input
                        type="text"
                        id="tenantId"
                        name="tenantId"
                        className="form-control"
                        value={formData.tenantId}
                        onChange={handleChange}
                        required
                    />
                </div>

                <div className="mb-3">
                    <label htmlFor="partyId" className="form-label">Party ID:</label>
                    <input
                        type="text"
                        id="partyId"
                        name="partyId"
                        className="form-control"
                        value={formData.partyId}
                        readOnly
                        aria-readonly="true"
                    />
                </div>

                {featureFlags?.authMode === 'oauth2' && (
                    <>
                        <div className="mb-3">
                            <label htmlFor="clientId" className="form-label">Client ID:</label>
                            <input
                                type="text"
                                id="clientId"
                                name="clientId"
                                className="form-control"
                                value={formData.clientId}
                                onChange={handleChange}
                                required
                            />
                        </div>
                        <div className="mb-3">
                            <label htmlFor="issuerUrl" className="form-label">Issuer URL:</label>
                            <input
                                type="text"
                                id="issuerUrl"
                                name="issuerUrl"
                                className="form-control"
                                value={formData.issuerUrl}
                                onChange={handleChange}
                                required
                            />
                        </div>
                    </>
                )}

                <div className="mb-3">
                    <label htmlFor="walletUrl" className="form-label">Wallet URL:</label>
                    <input
                        type="text"
                        id="walletUrl"
                        name="walletUrl"
                        className="form-control"
                        value={formData.walletUrl}
                        onChange={handleChange}
                    />
                </div>

                {featureFlags?.authMode === 'shared-secret' && (
                    <div className="mb-3">
                        <label htmlFor="users" className="form-label">Users (comma-separated):</label>
                        <input
                            type="text"
                            id="users"
                            name="users"
                            className="form-control"
                            value={Array.isArray(formData.users) ? formData.users.join(', ') : (formData.users ?? '')}
                            onChange={handleChange}
                        />
                    </div>
                )}

                <button type="submit" className="btn btn-primary" disabled={isSubmittingBlocked}>
                    Submit
                </button>
                {isSubmittingBlocked && <div className="form-text">Waiting for Party ID…</div>}
            </form>

            <div className="mt-4">
                <h3>Existing Tenant Registrations</h3>
                <table className="table nowrap">
                    <thead>
                    <tr>
                        <th>Tenant ID</th>
                        <th>Party ID</th>
                        {featureFlags?.authMode === 'oauth2' && (
                            <>
                                <th>Client ID</th>
                                <th>Issuer URL</th>
                            </>
                        )}
                        <th>Wallet URL</th>
                        {featureFlags?.authMode === 'shared-secret' && <th>Users</th>}
                        <th>Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {registrations.map((r) => (
                        <tr key={r.tenantId}>
                            <td>{r.tenantId}</td>
                            <td>{r.partyId}</td>
                            {featureFlags?.authMode === 'oauth2' && (
                                <>
                                    <td>{r.clientId}</td>
                                    <td>{r.issuerUrl}</td>
                                </>
                            )}
                            <td>{r.walletUrl}</td>
                            {featureFlags?.authMode === 'shared-secret' && <td>{Array.isArray(r.users) ? r.users.join(', ') : r.users}</td>}
                            <td>
                                <button
                                    className="btn btn-danger"
                                    disabled={r.internal}
                                    onClick={() => handleDelete(r.tenantId)}
                                >
                                    Delete
                                </button>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    )
}

export default TenantRegistrationView