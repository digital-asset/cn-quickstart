// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import {expect, Page} from '@playwright/test';
import type {APIRequestContext} from 'playwright-core';

/**
 * Returns a visible locator by the given selector. Fails if the element is not visible.
 */
export async function getVisibleLocator(
    page: Page,
    selector: string,
    message?: string
) {
    const locator = page.locator(selector);
    await expect(locator, message).toBeVisible();
    return locator;
}

/**
 * Retrieves the value of an environment variable by name, removing any `#` comments from the value.
 *
 * Unlike Docker Compose, docker's (standalone) --env-file option does not support inline comments, neither does node's
 * --env-file option.
 */
export function getEnv(name: string): string | undefined {
    const rawValue = process.env[name];
    if (!rawValue) {
        return undefined;
    }
    return rawValue.replace(/\s*#.*$/, '').trim();
}

/**
 * Requests an admin token using the client credentials grant type.
 */
export async function getAdminToken(
    request: APIRequestContext,
    clientSecret: string,
    clientId: string,
    tenant: string
): Promise<string> {
    const response = await request.post(`http://localhost:8082/realms/${tenant}/protocol/openid-connect/token`, {
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        form: {
            client_id: clientId,
            client_secret: clientSecret,
            grant_type: 'client_credentials',
            scope: 'openid'
        }
    });
    if (!response.ok()) {
        throw new Error(
            `Failed to get admin token: ${response.status()} ${response.statusText()}`
        );
    }
    const data = await response.json();
    return data.access_token;
}

/**
 * Requests a user token from Keycloak using the environment variables set for the app-user wallet admin.
 */
export async function getUserToken(request: APIRequestContext): Promise<string> {
    const user = getEnv('AUTH_APP_USER_WALLET_ADMIN_USER_NAME')!;
    const password = getEnv('AUTH_APP_USER_WALLET_ADMIN_USER_PASSWORD')!;
    const clientId = getEnv('AUTH_APP_USER_AUTO_CONFIG_CLIENT_ID')!;
    const tokenUrl = 'http://localhost:8082/realms/AppUser/protocol/openid-connect/token';
    const response = await request.post(tokenUrl, {
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        form: {
            client_id: clientId,
            username: user,
            password: password,
            grant_type: 'password',
            scope: 'openid'
        }
    });
    if (!response.ok()) {
        throw new Error(
            `Failed to get user token: ${response.status()} ${response.statusText()}`
        );
    }
    const data = await response.json();
    return data.access_token;
}

/**
 * Looks up the primary party for a given user on a given participant.
 */
export async function getUserParty(
    request: APIRequestContext,
    token: string,
    userId: string,
    port: number
): Promise<string> {
    const response = await request.get(`http://localhost:${port}/v2/users/${userId}`, {
        headers: {
            Authorization: `Bearer ${token}`,
        },
    });
    if (!response.ok()) {
        throw new Error(
            `Failed to get user party: ${response.status()} ${response.statusText()}`
        );
    }
    const data = await response.json();
    return data.user.primaryParty;
}

/**
 * Retrieves the DSO party identifier.
 */
export async function getDSOPartyId(
    request: APIRequestContext,
    token: string,
    port: number
): Promise<string> {
    const response = await request.get(`http://localhost:${port}/api/validator/v0/scan-proxy/dso-party-id`, {
        headers: {
            Authorization: `Bearer ${token}`,
        },
    });
    if (!response.ok()) {
        throw new Error(
            `Failed to get DSO party ID: ${response.status()} ${response.statusText()}`
        );
    }
    const data = await response.json();
    return data.dso_party_id;
}

/**
 * Sends an AppInstallRequest creation command.
 */
export async function createAppInstallRequest(
    request: APIRequestContext,
    token: string,
    dsoParty: string,
    appUserParty: string,
    appProviderParty: string,
    participantUserId: string,
    id: number,
): Promise<void> {
    const body = {
        commands: [
            {
                CreateCommand: {
                    templateId: '#quickstart-licensing:Licensing.AppInstall:AppInstallRequest',
                    createArguments: {
                        dso: dsoParty,
                        provider: appProviderParty,
                        user: appUserParty,
                        meta: {
                            values: []
                        }
                    }
                }
            }
        ],
        workflowId: 'create-app-install-request',
        applicationId: participantUserId,
        commandId: `create-app-install-request-${id}`,
        deduplicationPeriod: {
            Empty: {}
        },
        actAs: [appUserParty],
        readAs: [appUserParty],
        submissionId: 'create-app-install-request',
        disclosedContracts: [],
        domainId: '',
        packageIdSelectionPreference: []
    };

    const response = await request.post(`http://localhost:2`+ getEnv('PARTICIPANT_JSON_API_PORT') +`/v2/commands/submit-and-wait`, {
        headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json',
        },
        data: body
    });

    if (response.status() !== 200) {
        const responseBody = await response.text();
        throw new Error(`Request failed with HTTP status code ${response.status()}\nResponse body: ${responseBody}`);
    }
}

/**
 * Creates multiple AppInstallRequests, retrieving DSO and user parties dynamically.
 */
export async function createAppInstallRequests(request: APIRequestContext): Promise<void> {
    // Retrieve admin tokens for the participant that hosts the app user
    const appUserParticipantAdminToken = await getAdminToken(
        request,
        getEnv('AUTH_APP_USER_VALIDATOR_CLIENT_SECRET') || '',
        getEnv('AUTH_APP_USER_VALIDATOR_CLIENT_ID') || '',
        'AppUser'
    );
    const appUserParty = await getUserParty(
        request,
        appUserParticipantAdminToken,
        getEnv('AUTH_APP_USER_VALIDATOR_USER_ID') || '',
        Number('2' + getEnv('PARTICIPANT_JSON_API_PORT'))
    );

    // Retrieve admin tokens for the participant that hosts the app provider
    const appProviderParticipantAdminToken = await getAdminToken(
        request,
        getEnv('AUTH_APP_PROVIDER_VALIDATOR_CLIENT_SECRET') || '',
        getEnv('AUTH_APP_PROVIDER_VALIDATOR_CLIENT_ID') || '',
        'AppProvider'
    );
    const appProviderParty = await getUserParty(
        request,
        appProviderParticipantAdminToken,
        getEnv('AUTH_APP_PROVIDER_VALIDATOR_USER_ID') || '',
        Number('3' + getEnv('PARTICIPANT_JSON_API_PORT'))
    );

    // Retrieve a token for the wallet admin user and fetch the DSO party
    const walletAdminToken = await getUserToken(request);
    const dsoParty = await getDSOPartyId(request, walletAdminToken, 25003);

    // This is used as the applicationId in commands
    const participantUserId = getEnv('AUTH_APP_USER_WALLET_ADMIN_USER_ID') || 'auth-app-user-wallet-admin';

    console.log(`DSO party: ${dsoParty}`);
    console.log(`App user party: ${appUserParty}`);
    console.log(`App provider party: ${appProviderParty}`);

    const requestCount = 4;
    for (let i = 0; i < requestCount; i++) {
        console.log(`Submitting app install request ${i + 1} of ${requestCount}`);
        await createAppInstallRequest(
            request,
            walletAdminToken,
            dsoParty,
            appUserParty,
            appProviderParty,
            participantUserId,
            i,
        );
    }
}