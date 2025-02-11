// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "spring.security.oauth2.client")
public class CustomClientRegistrationProperties {
    private Map<String, String> walletUrls = new HashMap<>();

    public Map<String, String> getWalletUrls() {
        return walletUrls;
    }

    public void setWalletUrls(Map<String, String> walletUrls) {
        this.walletUrls = walletUrls;
    }
}
