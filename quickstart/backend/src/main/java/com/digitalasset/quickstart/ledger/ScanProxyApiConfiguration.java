// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.ledger;

import com.digitalasset.quickstart.config.LedgerConfig;
import com.digitalasset.quickstart.security.TokenProvider;
import com.digitalasset.quickstart.validatorproxy.client.ApiClient;
import com.digitalasset.quickstart.validatorproxy.client.api.ScanProxyApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScanProxyApiConfiguration {

    @Bean
    public ScanProxyApi scanProxyApi(TokenProvider tokenProvider, LedgerConfig ledgerConfig) {
        ApiClient apiClient = new ApiClient();
        apiClient.updateBaseUri(ledgerConfig.getValidatorUri());
        apiClient.setRequestInterceptor(requestBuilder -> {
            requestBuilder.header("Authorization", "Bearer " + tokenProvider.getToken());
        });
        return new ScanProxyApi(apiClient);
    }
}
