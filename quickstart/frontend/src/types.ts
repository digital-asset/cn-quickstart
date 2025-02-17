// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

export interface LoginLink {
    name: string;
    url: string;
}

export interface AuthenticatedUser {
    name: string;
    party: string;
    roles: string[];
    isAdmin: boolean;
    walletUrl: string;
}

export interface Contract {
    templateFqn: string;
    payloadType: string;
    createEventId: string;
    createdAtOffset: string;
    archiveEventId: string;
    archivedAtOffset: string;
    contractId: string;
    observers: string[];
    signatories: string[];
    payload: any;
    contractKey: any;
}

export interface ApiClient {
    // User & Auth
    getAuthenticatedUser(): Promise<{ data: AuthenticatedUser }>;
    listLinks(): Promise<{ data: LoginLink[] }>;

    // Assets
    listAssets(): Promise<{ data: Contract[] }>;
    createAsset(
        params: undefined,
        body: { label: string; owner: string }
    ): Promise<void>;
    archiveAsset(params: { contractId: string }): Promise<void>;
    changeAssetLabel(
        params: { contractId: string },
        body: { newLabel: string }
    ): Promise<void>;

    // AppInstallRequests
    listAppInstallRequests(): Promise<{ data: AppInstallRequest[] }>;
    createAppInstallRequest(
        params: { commandId: string }
    ): Promise<{ data: AppInstallRequest }>;
    acceptAppInstallRequest(
        params: { contractId: string; commandId: string },
        body: AppInstallRequestAccept
    ): Promise<{ data: AppInstall }>;
    rejectAppInstallRequest(
        params: { contractId: string; commandId: string },
        body: AppInstallRequestReject
    ): Promise<void>;
    cancelAppInstallRequest(
        params: { contractId: string; commandId: string },
        body: AppInstallRequestCancel
    ): Promise<void>;

    // AppInstalls
    createLicense(
        params: { contractId: string; commandId: string },
        body: AppInstallCreateLicenseRequest
    ): Promise<{ data: AppInstallCreateLicenseResult }>;
    cancelAppInstall(
        params: { contractId: string; commandId: string },
        body: AppInstallCancel
    ): Promise<void>;
    listAppInstalls(): Promise<{ data: AppInstall[] }>;

    // Licenses
    listLicenses(): Promise<{ data: License[] }>;

    // License renewal-related endpoints
    renewLicense(
        params: { contractId: string; commandId: string },
        body: LicenseRenewRequest
    ): Promise<{ data: LicenseRenewResponse }>;

    listLicenseRenewalRequests(): Promise<{ data: LicenseRenewalRequest[] }>;

    completeLicenseRenewal(
        params: { contractId: string; commandId: string },
        body: LicenseRenewalRequestComplete
    ): Promise<{ data: License }>;

    expireLicense(
        params: { contractId: string; commandId: string },
        body: LicenseExpireRequest
    ): Promise<string>;
}

export interface Metadata {
    data?: Record<string, string>;
}

// AppInstallRequest-related interfaces
export interface AppInstallRequest {
    contractId: string;
    dso: string;
    provider: string;
    user: string;
    meta: Metadata;
}

export interface AppInstallRequestCreation {}

export interface AppInstallRequestAccept {
    installMeta: Metadata;
    meta: Metadata;
}

export interface AppInstallRequestReject {
    meta: Metadata;
}

export interface AppInstallRequestCancel {
    meta: Metadata;
}

export interface AppInstall {
    contractId: string;
    dso: string;
    provider: string;
    user: string;
    meta: Metadata;
    numLicensesCreated: number;
    licenseNum: number;
}

export interface LicenseParams {
    meta: Metadata;
}

export interface AppInstallCreateLicenseRequest {
    params: LicenseParams;
}

export interface AppInstallCreateLicenseResult {
    installId: string;
    licenseId: string;
}

export interface AppInstallCancel {
    meta: Metadata;
}

// License-related interfaces
export interface License {
    contractId: string;
    dso: string;
    provider: string;
    user: string;
    params: LicenseParams;
    expiresAt: string; // ISO 8601 datetime string
    licenseNum: number;
}

// License renewal-related interfaces

export interface LicenseRenewRequest {
    licenseFeeCc: number;
    licenseExtensionDuration: string;     // e.g. "P30D"
    paymentAcceptanceDuration: string;
    description: string;
}

export interface LicenseRenewalRequest {
    contractId: string;
    provider: string;
    user: string;
    dso: string;
    licenseNum: number;
    licenseFeeCc: number;
    licenseExtensionDuration: string; // "RelTime" as a string (e.g. "P30D")
    reference: string;
}

export interface LicenseRenewResponse {
    renewalOffer: LicenseRenewalRequest;
    paymentRequest: AppPaymentRequest;
}

export interface LicenseRenewalRequestComplete {
    licenseCid: string;
    reference: string;
}

export interface LicenseExpireRequest {
    meta: Metadata;
}

// Payment-related interfaces

export interface AppPaymentRequest {
    contractId: string;
    provider: string;
    dso: string;
    sender: string;
    receiverAmounts: ReceiverAmount[];
    description: string;
    expiresAt: string; // ISO 8601 datetime
}

export interface ReceiverAmount {
    receiver: string;
    amount: PaymentAmount;
}

export interface PaymentAmount {
    amount: number;
    unit: string;
}
