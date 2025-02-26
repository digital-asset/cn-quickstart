// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React from 'react';
import './App.css';
import { Routes, Route } from 'react-router-dom';
import { ToastProvider } from './stores/toastStore';
import HomeView from './views/HomeView';
import TenantRegistrationView from './views/TenantRegistrationView.tsx';
import LoginView from './views/LoginView';
import { UserProvider } from './stores/userStore';
import Header from './components/Header';
import ToastNotification from './components/ToastNotification';
import AppInstallsView from "./views/AppInstallsView.tsx";
import LicensesView from './views/LicensesView';
import { LicenseProvider } from './stores/licenseStore';
import {AppInstallProvider} from "./stores/appInstallStore.tsx";
import LicenseRenewalRequestsView from "./views/LicenseRenewalRequestsView.tsx";
import {TenantRegistrationProvider} from "./stores/tenantRegistrationStore.tsx";

const App: React.FC = () => {
    const AppProviders = composeProviders(
        ToastProvider,
        UserProvider,
        TenantRegistrationProvider,
        AppInstallProvider,
        LicenseProvider
    );

    return (
        <AppProviders>
            <Header />
            <main className="container mt-4">
                <Routes>
                    <Route path="/" element={<HomeView />} />
                    <Route path="/tenants" element={<TenantRegistrationView />} />
                    <Route path="/login" element={<LoginView />} />
                    <Route path="/app-installs" element={<AppInstallsView />} />
                    <Route path="/licenses" element={<LicensesView />} />
                    <Route path="/license-renewal-requests" element={<LicenseRenewalRequestsView />} />
                </Routes>
            </main>
            <ToastNotification />
        </AppProviders>
    );
};

const composeProviders = (...providers: React.ComponentType<{ children: React.ReactNode }>[]) => {
    return providers.reduce(
        (AccumulatedProviders, CurrentProvider) => {
            return ({ children }: { children: React.ReactNode }) => (
                <AccumulatedProviders>
                    <CurrentProvider>
                        {children}
                    </CurrentProvider>
                </AccumulatedProviders>
            );
        },
        ({ children }: { children: React.ReactNode }) => <>{children}</>
    );
};

export default App;
