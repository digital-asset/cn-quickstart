// Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React, { createContext, useContext, useState, useCallback } from 'react';
import { useToast } from './toastStore';
import api from '../api';
import { useNavigate } from 'react-router-dom';
import type { AuthenticatedUser, Client } from "../openapi.d.ts";

interface UserContextType {
    user: AuthenticatedUser | null;
    loading: boolean;
    fetchUser: () => Promise<void>;
    clearUser: () => void;
    logout: () => Promise<void>;
}

const UserContext = createContext<UserContextType | undefined>(undefined);

export const UserProvider = ({ children }: { children: React.ReactNode }) => {
    const [user, setUser] = useState<AuthenticatedUser | null>(null);
    const [loading, setLoading] = useState<boolean>(true);
    const toast = useToast();
    const navigate = useNavigate();

    const fetchUser = useCallback(async () => {
        setLoading(true);
        try {
            const client: Client = await api.getClient();
            const response = await client.getAuthenticatedUser();
            setUser(response.data);
        } catch (error) {
            if ((error as any)?.response?.status === 401) {
                setUser(null);
            } else {
                toast.displayError('Error fetching user');
            }
        } finally {
            setLoading(false);
        }
    }, [setUser, setLoading, toast]);

    const clearUser = useCallback(() => {
        setUser(null);
    }, [setUser]);

    const getCsrfToken = (): string => {
        const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
        return match ? decodeURIComponent(match[1]) : '';
    };

    const logout = useCallback(async () => {
        try {
            const response = await fetch('/api/logout', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-XSRF-TOKEN': getCsrfToken(),
                },
            });
            if (response.ok) {
                clearUser();
                navigate('/');
            } else {
                toast.displayError('Error logging out');
            }
        } catch (error) {
            toast.displayError('Error logging out');
        }
    }, [clearUser, toast, navigate, getCsrfToken]);

    return (
        <UserContext.Provider value={{ user, loading, fetchUser, clearUser, logout }}>
            {children}
        </UserContext.Provider>
    );
};

export const useUserStore = () => {
    const context = useContext(UserContext);
    if (context === undefined) {
        throw new Error('useUser must be used within a UserProvider');
    }
    return context;
};
