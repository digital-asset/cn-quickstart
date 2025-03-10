// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React, {useEffect, useState} from 'react';
import {useToast} from '../stores/toastStore';
import api from '../api';
import {Client, LoginLink} from "../openapi";

const LoginView: React.FC = () => {
    const [loginLinks, setLoginLinks] = useState<LoginLink[]>([]);
    const toast = useToast();

    useEffect(() => {
        const fetchLoginLinks = async () => {
            try {
                const client: Client = await api.getClient();
                const response = await client.listLinks();
                setLoginLinks(response.data);
            } catch (error) {
                toast.displayError('Error fetching login links');
            }
        };
        fetchLoginLinks();
    }, [toast]);

    return (
        <div className="container">
            <h2>Login with OAuth 2.0</h2>
            <table className="table table-striped">
                <tbody>
                {loginLinks.map((link) => (
                    <tr key={link.url}>
                        <td>
                            <a className="btn btn-link" href={link.url}>{link.name}</a>
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>
            {/*TODO get username/password from .env*/}
            <div>AppProvider user: bob, password: secret</div>
            <div>AppUser user: alice, password: secret</div>
        </div>
    );
};

export default LoginView;
