// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates.
// SPDX-License-Identifier: 0BSD

import React, { useEffect, useState } from 'react'
import {
    useTenantRegistrationStore,
    TenantRegistration,
} from '../stores/tenantRegistrationStore'

const TenantRegistrationView: React.FC = () => {
    const {
        registrations,
        fetchTenantRegistrations,
        createTenantRegistration,
        deleteTenantRegistration,
    } = useTenantRegistrationStore()

    const [formData, setFormData] = useState<TenantRegistration>({
        clientId: '',
        clientSecret: '',
        scope: '',
        authorizationUri: '',
        tokenUri: '',
        jwkSetUri: '',
        party: '',
        preconfigured: false,
        walletUrl: '',
    })

    useEffect(() => {
        fetchTenantRegistrations()
    }, [fetchTenantRegistrations])

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target
        setFormData((prev) => ({
            ...prev,
            [name]: value,
        }))
    }

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        await createTenantRegistration(formData)
        setFormData({
            clientId: '',
            clientSecret: '',
            scope: '',
            authorizationUri: '',
            tokenUri: '',
            jwkSetUri: '',
            party: '',
            preconfigured: false,
            walletUrl: '',
        })
    }

    const handleDelete = async (clientId: string) => {
        if (window.confirm('Are you sure you want to delete this tenant registration?')) {
            await deleteTenantRegistration(clientId)
        }
    }

    return (
        <div>
            <form onSubmit={handleSubmit}>
                <div className="mb-3">
                    <label htmlFor="clientId" className="form-label">
                        Client ID:
                    </label>
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
                    <label htmlFor="clientSecret" className="form-label">
                        Client Secret:
                    </label>
                    <input
                        type="text"
                        id="clientSecret"
                        name="clientSecret"
                        className="form-control"
                        value={formData.clientSecret}
                        onChange={handleChange}
                    />
                </div>
                <div className="mb-3">
                    <label htmlFor="scope" className="form-label">
                        Scope:
                    </label>
                    <input
                        type="text"
                        id="scope"
                        name="scope"
                        className="form-control"
                        value={formData.scope}
                        onChange={handleChange}
                        required
                    />
                </div>
                <div className="mb-3">
                    <label htmlFor="authorizationUri" className="form-label">
                        Authorization URI:
                    </label>
                    <input
                        type="text"
                        id="authorizationUri"
                        name="authorizationUri"
                        className="form-control"
                        value={formData.authorizationUri}
                        onChange={handleChange}
                        required
                    />
                </div>
                <div className="mb-3">
                    <label htmlFor="tokenUri" className="form-label">
                        Token URI:
                    </label>
                    <input
                        type="text"
                        id="tokenUri"
                        name="tokenUri"
                        className="form-control"
                        value={formData.tokenUri}
                        onChange={handleChange}
                        required
                    />
                </div>
                <div className="mb-3">
                    <label htmlFor="jwkSetUri" className="form-label">
                        JWK Set URI:
                    </label>
                    <input
                        type="text"
                        id="jwkSetUri"
                        name="jwkSetUri"
                        className="form-control"
                        value={formData.jwkSetUri}
                        onChange={handleChange}
                        required
                    />
                </div>
                <div className="mb-3">
                    <label htmlFor="party" className="form-label">
                        Party:
                    </label>
                    <input
                        type="text"
                        id="party"
                        name="party"
                        className="form-control"
                        value={formData.party}
                        onChange={handleChange}
                        required
                    />
                </div>
                <div className="mb-3">
                    <label htmlFor="walletUrl" className="form-label">
                        Wallet URL:
                    </label>
                    <input
                        type="text"
                        id="walletUrl"
                        name="walletUrl"
                        className="form-control"
                        value={formData.walletUrl}
                        onChange={handleChange}
                    />
                </div>
                <button type="submit" className="btn btn-primary">
                    Submit
                </button>
            </form>

            <div className="mt-4">
                <h3>Existing Tenant/OAuth2 Registrations</h3>
                <table className="table">
                    <thead>
                    <tr>
                        <th>Client ID</th>
                        <th>Party</th>
                        <th>Wallet URL</th>
                        <th>Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {registrations.map((registration, index) => (
                        <tr key={index}>
                            <td>{registration.clientId}</td>
                            <td>{registration.party}</td>
                            <td>{registration.walletUrl}</td>
                            <td>
                                <button
                                    className="btn btn-danger"
                                    disabled={registration.preconfigured}
                                    onClick={() => handleDelete(registration.clientId)}
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
