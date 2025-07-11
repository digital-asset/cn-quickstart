// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates.
// SPDX-License-Identifier: 0BSD

import React, { useEffect, useState } from 'react'
import {
    useTenantRegistrationStore,
    TenantRegistration,
} from '../stores/tenantRegistrationStore'

import {useToast} from '../stores/toastStore';
import api from '../api';
import {Client, FeatureFlags} from "../openapi";


const TenantRegistrationView: React.FC = () => {
    const {
        registrations,
        fetchTenantRegistrations,
        createTenantRegistration,
        deleteTenantRegistration,
    } = useTenantRegistrationStore()

    const [formData, setFormData] = useState<TenantRegistration>({
        tenantId: '',
        partyId: '',
        clientId: '',
        issuerUrl: '',
        internal: false,
        walletUrl: '',
        users: []
    })

    const toast = useToast();
    const [featureFlags, setFeatureFlags] = useState<FeatureFlags | null>(null);

    const fetchFeatureFlags = async () => {
        try {
            const client: Client = await api.getClient();
            const response = await client.getFeatureFlags();
            setFeatureFlags(response.data);
        } catch (error) {
            toast.displayError('Error fetching feature flags');
        }
    };

    useEffect(() => {
        fetchFeatureFlags();
        fetchTenantRegistrations()
    }, [fetchTenantRegistrations])

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target
        setFormData((prev) => ({
            ...prev,
            [name]: name === 'users' ? value.split(',').map(user => user.trim()) : value,
        }))
    }

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        await createTenantRegistration(formData)
        setFormData({
            tenantId: '',
            partyId: '',
            clientId: '',
            issuerUrl: '',
            internal: false,
            walletUrl: '',
            users: []

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
                    <label htmlFor="tenantId" className="form-label">
                        Tenant ID:
                    </label>
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
                    <label htmlFor="partyId" className="form-label">
                        PartyId:
                    </label>
                    <input
                        type="text"
                        id="partyId"
                        name="partyId"
                        className="form-control"
                        value={formData.partyId}
                        onChange={handleChange}
                        required
                    />
                </div>
                {featureFlags?.authMode === 'oauth2' && (
                    <>
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
                            <label htmlFor="issuerUrl" className="form-label">
                                Issuer URL:
                            </label>
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
                {featureFlags?.authMode === 'shared-secret' && (
                  <div className="mb-3">
                      <label htmlFor="users" className="form-label">
                          Users (comma-separated):
                      </label>
                      <input
                          type="text"
                          id="users"
                          name="users"
                          className="form-control"
                          value={formData.users}
                          onChange={handleChange}
                      />
                  </div>
                )}
                <button type="submit" className="btn btn-primary">
                    Submit
                </button>
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
                    {registrations.map((registration, index) => (
                        <tr key={index}>
                            <td>{registration.tenantId}</td>
                            <td>{registration.partyId}</td>
                            {featureFlags?.authMode === 'oauth2' && (
                                <>
                                    <td>{registration.clientId}</td>
                                    <td>{registration.issuerUrl}</td>
                                </>
                            )}
                            <td>{registration.walletUrl}</td>
                            {featureFlags?.authMode === 'shared-secret' && <td>{registration.users}</td>}
                            <td>
                                <button
                                    className="btn btn-danger"
                                    disabled={registration.internal}
                                    onClick={() => handleDelete(registration.tenantId)}
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
