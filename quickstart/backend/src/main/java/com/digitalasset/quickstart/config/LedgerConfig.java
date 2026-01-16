// Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ledger")
public class LedgerConfig {

    private String host = "localhost";
    private int port = 6865;
    private String applicationId;
    private String registryBaseUri;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getRegistryBaseUri() {
        return registryBaseUri;
    }

    public void setRegistryBaseUri(String registryBaseUri) {
        this.registryBaseUri = registryBaseUri;
    }
}
