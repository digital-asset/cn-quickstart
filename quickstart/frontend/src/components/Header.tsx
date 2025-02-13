// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React from 'react';
import {Link} from 'react-router-dom';
import {useUserStore} from '../stores/userStore';

const Header: React.FC = () => {
    return (
        <header>
            <nav className="navbar navbar-expand-lg navbar-light bg-light">
                <div className="container-fluid">
                    <a className="navbar-brand" href="#">
                        Canton Network Quickstart
                    </a>
                    <button
                        className="navbar-toggler"
                        type="button"
                        data-bs-toggle="collapse"
                        data-bs-target="#navbarNav"
                        aria-controls="navbarNav"
                        aria-expanded="false"
                        aria-label="Toggle navigation"
                    >
                        <span className="navbar-toggler-icon"></span>
                    </button>
                    <div className="collapse navbar-collapse" id="navbarNav">
                        <AuthenticatedLinks/>
                    </div>
                </div>
            </nav>
        </header>
    );
};

const AuthenticatedLinks: React.FC = () => {
    const {user, loading, fetchUser, logout} = useUserStore();

    React.useEffect(() => {
        fetchUser();
    }, [fetchUser]);

    if (loading) {
        return <div>Loading...</div>;
    }

    if (user === null) {
        return (
            <ul className="navbar-nav ms-auto mb-2 mb-lg-0">
                <li className="nav-item">
                    <Link className="nav-link" to="/login">
                        Login
                    </Link>
                </li>
            </ul>
        );
    } else {
        return (
            <>
                <ul className="navbar-nav me-auto mb-2 mb-lg-0">
                    <li className="nav-item">
                        <Link className="nav-link" to="/">
                            Home
                        </Link>
                    </li>
                    {/*{user.isAdmin && (*/}
                    <li className="nav-item">
                        <Link className="nav-link" to="/app-install-requests">
                            AppInstallRequests
                        </Link>
                    </li>
                    <li className="nav-item">
                        <Link className="nav-link" to="/app-installs">
                            AppInstalls
                        </Link>
                    </li>
                    <li className="nav-item">
                        <Link className="nav-link" to="/licenses">
                            Licenses
                        </Link>
                    </li>
                    <li className="nav-item">
                        <Link className="nav-link" to="/license-renewal-requests">
                            LicenseRenewalRequests
                        </Link>
                    </li>
                    {user.isAdmin && (
                        <li className="nav-item">
                            <Link className="nav-link" to="/tenants">
                                Tenants
                            </Link>
                        </li>
                    )}
                </ul>
                <ul className="navbar-nav ms-auto mb-2 mb-lg-0">
                    <li className="nav-item">
                        <span className="nav-link" style={{fontWeight: 'bold'}}>
                            {user.name}
                        </span>
                    </li>
                    <li className="nav-item">
                        <button className="nav-link btn btn-link" onClick={logout}>
                            Logout
                        </button>
                    </li>
                </ul>
            </>
        );
    }
};

export default Header;
