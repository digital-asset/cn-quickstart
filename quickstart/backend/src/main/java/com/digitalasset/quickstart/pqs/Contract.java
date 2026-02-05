// Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.pqs;

import com.digitalasset.transcode.java.ContractId;
import com.digitalasset.transcode.java.Template;

public class Contract<T extends Template> {
    public final ContractId<T> contractId;
    public final T payload;

    public Contract(ContractId<T> contractId, T payload) {
        this.contractId = contractId;
        this.payload = payload;
    }
}
