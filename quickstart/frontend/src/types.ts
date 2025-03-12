import type { Metadata } from './openapi';

export type AppInstallStatus = 'REQUEST' | 'INSTALL';

export interface AppInstallUnified {
    status: AppInstallStatus;
    contractId: string;
    dso: string;
    provider: string;
    user: string;
    meta: Metadata;
    numLicensesCreated: number;
}
