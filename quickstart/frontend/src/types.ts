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
import {ContractId, Party, Numeric} from "../generated/daml";

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

export interface ApiClient {
    // User & Auth
    getAuthenticatedUser(): Promise<{ data: AuthenticatedUser }>;
    listLinks(): Promise<{ data: LoginLink[] }>;

    // AppInstallRequests
    listAppInstallRequests(): Promise<{ data: Contract<AppInstallRequest>[] }>;
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

    listAppInstalls(): Promise<{ data: Contract<AppInstall>[] }>;

    // Licenses
    listLicenses(): Promise<{ data: Contract<License>[] }>;

    // License renewal-related endpoints
    renewLicense(
        params: { contractId: string; commandId: string },
        body: License_Renew
    ): Promise<{ data: LicenseRenewResponse }>;

    listLicenseRenewalRequests(): Promise<{ data: Contract<LicenseRenewalRequest>[] }>;

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
    return { microseconds: str }
}

export function toNumeric(num: number): Numeric { return num.toString()}

export type Contract<A> = A & {
    templateFqn: string;
    payloadType: string;
    createEventId: string;
    createdAtOffset: string;
    archiveEventId: string;
    archivedAtOffset: string;
    contractId: ContractId<A>;
    observers: Party[];
    signatories: Party[];
}
