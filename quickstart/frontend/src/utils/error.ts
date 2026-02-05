// Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import type { ErrorResponse } from "../openapi.d.ts";
import type { AxiosError } from 'axios';
import { useToast } from '../stores/toastStore';


function isAxiosErrorWithErrorResponse(
    err: unknown
): err is AxiosError<ErrorResponse> {
    return typeof (err as any)?.isAxiosError === 'boolean';
}

function extractError(err: unknown): { status?: number; message?: string } {
    if (isAxiosErrorWithErrorResponse(err)) {
        const status = err.response?.status;
        const data = err.response?.data;
        return {
            status,
            message: data?.message ?? `HTTP ${status ?? 'Unknown error'}`
        };
    }
    // fallback
    const anyErr = err as any;
    return {
        status: anyErr?.status,
        message: anyErr?.message ?? 'Unexpected error',
    };
}

export function withErrorHandling(action: string) {
    const toast = useToast();

    function wrap<T extends (...args: any[]) => Promise<any>>(
        fn: T,
        onSuccess?: (result: Awaited<ReturnType<T>>) => void,
    ): (...args: Parameters<T>) => Promise<Awaited<ReturnType<T>> | void> {
        return async (...args: Parameters<T>): Promise<Awaited<ReturnType<T>> | void> => {
            try {
                const result = await fn(...args);
                onSuccess?.(result);
                return result;
            } catch (err) {
                const { status, message } = extractError(err);
                if (status === 400) return toast.displayError(message ? `${action} reason: ${message}` : `Invalid input in ${action}`);
                if (status === 401) return toast.displayError(message ? `${action} reason: ${message}` : `Unauthorized for ${action}`);
                if (status === 403) return toast.displayError(message ? `${action} reason: ${message}` : `Forbidden for ${action}`);
                if (status === 404) return toast.displayError(message ? `${action} reason: ${message}` : `Not Found for ${action}`);
                if (status === 409) return toast.displayError(message ? `${action} reason: ${message}` : `Conflict in ${action}`);
                toast.displayError(message ? `${action} reason: ${message}` : `Unexpected error for ${action}`);
            }
        };
    }
    return wrap;
}
