// Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

/**
 * Generate a random unique string (UUID) to be used as a commandId.
 * This must be generated outside of any retry logic so that the
 * same submission retains the same commandId for idempotence.
 */
export function generateCommandId(): string {
  // 1) If `crypto.randomUUID` is supported, use it (most modern browsers)
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }

  // 2) Otherwise, try using `crypto.getRandomValues` to generate a RFC4122 version 4 UUID
  if (typeof crypto !== 'undefined' && typeof crypto.getRandomValues === 'function') {
    const array = new Uint8Array(16);
    crypto.getRandomValues(array);

    // Per RFC 4122 4.4, set bits for version and `clock_seq_hi_and_reserved`
    // - Set bits 12-15 of time_hi_and_version to 0x4
    array[6] = (array[6] & 0x0f) | 0x40;
    // - Set bits 6-7 of clock_seq_hi_and_reserved to 0x2
    array[8] = (array[8] & 0x3f) | 0x80;

    const hex = [...array].map(b => b.toString(16).padStart(2, '0')).join('');
    return [
      hex.substring(0, 8),
      hex.substring(8, 12),
      hex.substring(12, 16),
      hex.substring(16, 20),
      hex.substring(20),
    ].join('-');
  }

  // 3) Fallback to a non-cryptographically-secure UUID-like string using Math.random
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
    const r = Math.random() * 16 | 0;
    const v = c === 'x' ? r : ((r & 0x3) | 0x8);
    return v.toString(16);
  });
}
