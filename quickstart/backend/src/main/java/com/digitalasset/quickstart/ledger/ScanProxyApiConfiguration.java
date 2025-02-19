// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.ledger;

import com.digitalasset.quickstart.validatorproxy.client.ApiClient;
import com.digitalasset.quickstart.validatorproxy.client.api.ScanProxyApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

@Configuration
public class ScanProxyApiConfiguration {

    private static final String CLIENT_REGISTRATION_ID = "AppProviderBackend-Participant"; // TODO change to validator when we have different audiences

    @Bean
    public ScanProxyApi scanProxyApi(OAuth2AuthorizedClientManager authorizedClientManager) {
        ApiClient apiClient = new ApiClient();
        apiClient.updateBaseUri("http://validator-app-provider:5003/api/validator"); // TODO: configure this properly
        apiClient.setRequestInterceptor(requestBuilder -> {
            OAuth2AuthorizeRequest req = OAuth2AuthorizeRequest.withClientRegistrationId(CLIENT_REGISTRATION_ID)
                    .principal("N/A")
                    .build();
            var authorizedClient = authorizedClientManager.authorize(req);
            if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                String accessToken = authorizedClient.getAccessToken().getTokenValue();
                requestBuilder.header("Authorization", "Bearer " + accessToken);
            } else {
                throw new IllegalStateException("Failed to obtain access token for ScanProxyApi");
            }
        });

        return new ScanProxyApi(apiClient);
    }
}
