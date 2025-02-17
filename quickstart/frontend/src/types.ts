// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import {
    AppInstall,
    AppInstall_CreateLicense_Result,
    AppInstallRequest
} from "../generated/quickstart_licensing/Licensing/AppInstall";
import AppInstallRequest_Accept = AppInstallRequest.AppInstallRequest_Accept;
import AppInstallRequest_Reject = AppInstallRequest.AppInstallRequest_Reject;
import AppInstallRequest_Cancel = AppInstallRequest.AppInstallRequest_Cancel;
import AppInstall_CreateLicense = AppInstall.AppInstall_CreateLicense;
import AppInstall_Cancel = AppInstall.AppInstall_Cancel;
import {License, LicenseRenewalRequest} from "../generated/quickstart_licensing/Licensing/License";
import License_Renew = License.License_Renew;
import License_Expire = License.License_Expire;
import {AppPaymentRequest} from "../generated/splice_wallet_payments/Splice/Wallet/Payment";
import {Metadata} from "../generated/quickstart_licensing/Licensing/Util";
import {RelTime} from "../generated/daml_stdlib_DA_Time_Types/DA/Time/Types";
import {ContractId, Party} from "../generated/daml";

export interface LoginLink {
    name: string;
    url: string;
}

export interface AuthenticatedUser {
    name: string;
    party: string;
    roles: string[];
    isAdmin: boolean;
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
    listAppInstallRequests(): Promise<{ data: Contract0<AppInstallRequest>[] }>;
    createAppInstallRequest(
        params: { commandId: string }
    ): Promise<{ data: AppInstallRequest }>;
    acceptAppInstallRequest(
        params: { contractId: string; commandId: string },
        body: AppInstallRequest_Accept
    ): Promise<{ data: AppInstall }>;
    rejectAppInstallRequest(
        params: { contractId: string; commandId: string },
        body: AppInstallRequest_Reject
    ): Promise<void>;
    cancelAppInstallRequest(
        params: { contractId: string; commandId: string },
        body: AppInstallRequest_Cancel
    ): Promise<void>;

    // AppInstalls
    createLicense(
        params: { contractId: string; commandId: string },
        body: AppInstall_CreateLicense
    ): Promise<{ data: AppInstall_CreateLicense_Result }>;

    cancelAppInstall(
        params: { contractId: string; commandId: string },
        body: AppInstall_Cancel
    ): Promise<void>;

    listAppInstalls(): Promise<{ data: Contract0<AppInstall>[] }>;

    // Licenses
    listLicenses(): Promise<{ data: Contract0<License>[] }>;

    // License renewal-related endpoints
    renewLicense(
        params: { contractId: string; commandId: string },
        body: License_Renew
    ): Promise<{ data: LicenseRenewResponse }>;

    listLicenseRenewalRequests(): Promise<{ data: Contract0<LicenseRenewalRequest>[] }>;

    completeLicenseRenewal(
        params: { contractId: string; commandId: string },
        body: LicenseRenewalRequestComplete
    ): Promise<{ data: License }>;

    expireLicense(
        params: { contractId: string; commandId: string },
        body: License_Expire
    ): Promise<string>;
}

export interface LicenseRenewResponse {
    renewalOffer: LicenseRenewalRequest;
    paymentRequest: AppPaymentRequest;
}

export interface LicenseRenewalRequestComplete {
    licenseCid: string;
    reference: string;
}

export function toMeta(rec: Record<string, any>): Metadata {
    return { values: Array.from(rec.entries()) }
}

export function toRelTime(str: string): RelTime {
    return { microseconds: 0 } // TODO
}

export type Contract0<a> = {
    templateFqn: string;
    payloadType: string;
    createEventId: string;
    createdAtOffset: string;
    archiveEventId: string;
    archivedAtOffset: string;
    contractId: ContractId<a>;
    observers: Party[];
    signatories: Party[];
    payload: a;
}
