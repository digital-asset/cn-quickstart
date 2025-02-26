// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React, { createContext, useContext, useState, useCallback } from 'react';
import api from '../api';
import { generateCommandId } from '../utils/commandId';
import { useToast } from './toastStore';
import type {
  AppInstall as ApiAppInstall,
  AppInstallRequest as ApiAppInstallRequest,
  AppInstallRequestAccept,
  AppInstallRequestReject,
  AppInstallRequestCancel,
  AppInstallCreateLicenseRequest,
  AppInstallCreateLicenseResult,
  AppInstallCancel,
  Client,
  Metadata,
} from '../openapi.d.ts';

import { AppInstallUnified } from '../types';

interface AppInstallState {
  // The unified list of items. Each item can be in "REQUEST" or "INSTALL" status.
  unifiedInstalls: AppInstallUnified[];
}

interface AppInstallContextType extends AppInstallState {
  fetchAll: () => Promise<void>;

  // Actions for items in "REQUEST" status:
  accept: (contractId: string, installMeta: Metadata, meta: Metadata) => Promise<void>;
  reject: (contractId: string, meta: Metadata) => Promise<void>;
  cancelRequest: (contractId: string, meta: Metadata) => Promise<void>;

  // Actions for items in "INSTALL" status:
  cancelInstall: (contractId: string, meta: Metadata) => Promise<void>;
  createLicense: (contractId: string, meta: Metadata) => Promise<AppInstallCreateLicenseResult | undefined>;
}

const AppInstallContext = createContext<AppInstallContextType | undefined>(undefined);

export const AppInstallProvider = ({ children }: { children: React.ReactNode }) => {
  const [unifiedInstalls, setUnifiedInstalls] = useState<AppInstallUnified[]>([]);
  const toast = useToast();

  /**
   * Fetch both AppInstallRequests and AppInstalls from the backend,
   * then merge them into a single array with a "status" property.
   */
  const fetchAll = useCallback(async () => {
    try {
      const client: Client = await api.getClient();

      // 1. Fetch requests
      const requestsResponse = await client.listAppInstallRequests();
      const requests: ApiAppInstallRequest[] = requestsResponse.data;

      // 2. Fetch installs
      const installsResponse = await client.listAppInstalls();
      const installs: ApiAppInstallRequest[] = installsResponse.data;

      // 3. Convert them to our unified model
      const unifiedRequests: AppInstallUnified[] = requests.map((r) => ({
        status: 'REQUEST',
        contractId: r.contractId,
        dso: r.dso,
        provider: r.provider,
        user: r.user,
        meta: r.meta,
        numLicensesCreated: 0,
      }));

      const unifiedInstalls: AppInstallUnified[] = (installs as unknown as ApiAppInstall[]).map((i) => ({
        status: 'INSTALL',
        contractId: i.contractId,
        dso: i.dso,
        provider: i.provider,
        user: i.user,
        meta: i.meta,
        numLicensesCreated: i.numLicensesCreated || 0,
      }));

      // 4. Merge them
      const combined = [...unifiedRequests, ...unifiedInstalls];

      // 5. Sort or do other transformations if needed
      // combined.sort((a, b) => a.contractId.localeCompare(b.contractId));

      setUnifiedInstalls(combined);
    } catch (error) {
      toast.displayError('Error fetching AppInstall data');
    }
  }, [toast]);

  /**
   * Accept (for requests)
   */
  const accept = useCallback(
      async (contractId: string, installMeta: Metadata, meta: Metadata) => {
        try {
          const client: Client = await api.getClient();
          const commandId = generateCommandId();
          await client.acceptAppInstallRequest({ contractId, commandId }, { installMeta, meta } as AppInstallRequestAccept);
          await fetchAll();
        } catch (error) {
          toast.displayError('Error accepting AppInstallRequest');
        }
      },
      [toast, fetchAll]
  );

  /**
   * Reject (for requests)
   */
  const reject = useCallback(
      async (contractId: string, meta: Metadata) => {
        try {
          const client: Client = await api.getClient();
          const commandId = generateCommandId();
          await client.rejectAppInstallRequest({ contractId, commandId }, { meta } as AppInstallRequestReject);
          await fetchAll();
        } catch (error) {
          toast.displayError('Error rejecting AppInstallRequest');
        }
      },
      [toast, fetchAll]
  );

  /**
   * Cancel (for requests)
   */
  const cancelRequest = useCallback(
      async (contractId: string, meta: Metadata) => {
        try {
          const client: Client = await api.getClient();
          const commandId = generateCommandId();
          await client.cancelAppInstallRequest({ contractId, commandId }, { meta } as AppInstallRequestCancel);
          await fetchAll();
        } catch (error) {
          toast.displayError('Error canceling AppInstallRequest');
        }
      },
      [toast, fetchAll]
  );

  /**
   * Cancel (for installs)
   */
  const cancelInstall = useCallback(
      async (contractId: string, meta: Metadata) => {
        try {
          const client: Client = await api.getClient();
          const commandId = generateCommandId();
          await client.cancelAppInstall({ contractId, commandId }, { meta } as AppInstallCancel);
          await fetchAll();
        } catch (error) {
          toast.displayError('Error canceling AppInstall');
        }
      },
      [toast, fetchAll]
  );

  /**
   * Create license (for installs)
   */
  const createLicense = useCallback(
      async (contractId: string, meta: Metadata) => {
        try {
          const client: Client = await api.getClient();
          const body: AppInstallCreateLicenseRequest = {
            params: { meta },
          };
          const commandId = generateCommandId();
          const response = await client.createLicense({ contractId, commandId }, body);
          await fetchAll();
          return response.data;
        } catch (error) {
          toast.displayError('Error creating License from AppInstall');
        }
      },
      [toast, fetchAll]
  );

  return (
      <AppInstallContext.Provider
          value={{
            unifiedInstalls,
            fetchAll,
            accept,
            reject,
            cancelRequest,
            cancelInstall,
            createLicense,
          }}
      >
        {children}
      </AppInstallContext.Provider>
  );
};

export const useAppInstallStore = () => {
  const context = useContext(AppInstallContext);
  if (context === undefined) {
    throw new Error('useAppInstallStore must be used within an AppInstallProvider');
  }
  return context;
};
