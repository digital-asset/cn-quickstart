// Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React, { createContext, useContext, useState, useCallback, useRef } from 'react'

interface ToastContextType {
    message: string
    show: boolean
    displayError: (message: string) => void
    displaySuccess: (message: string) => void
    hideError: () => void
}

interface ToastProviderProps {
    children: React.ReactNode
}

const ToastContext = createContext<ToastContextType | undefined>(undefined)

export const ToastProvider = ({ children }: ToastProviderProps) => {
    const [message, setMessage] = useState('')
    const [show, setShow] = useState(false)
    const timeoutIdRef = useRef<ReturnType<typeof setTimeout> | null>(null)

    const hideError = useCallback(() => {
        setMessage('')
        setShow(false)
        if (timeoutIdRef.current !== null) {
            clearTimeout(timeoutIdRef.current)
            timeoutIdRef.current = null
        }
    }, [])

    const displayError = useCallback(
        (msg: string) => {
            setMessage(`Error: ${msg}`)
            setShow(true)
            if (timeoutIdRef.current !== null) {
                clearTimeout(timeoutIdRef.current)
            }
            timeoutIdRef.current = setTimeout(() => {
                hideError()
            }, 10000)
        },
        [hideError]
    )

    const displaySuccess = useCallback(
        (msg: string) => {
            setMessage(`Success: ${msg}`)
            setShow(true)
            if (timeoutIdRef.current !== null) {
                clearTimeout(timeoutIdRef.current)
            }
            timeoutIdRef.current = setTimeout(() => {
                hideError()
            }, 5000) // Success messages could auto-hide faster
        },
        [hideError]
    )

    return (
        <ToastContext.Provider value={{ message, show, displayError, displaySuccess, hideError }}>
            {children}
        </ToastContext.Provider>
    )
}

export const useToast = () => {
    const context = useContext(ToastContext)
    if (context === undefined) {
        throw new Error('useToast must be used within a ToastProvider')
    }
    return context
}
