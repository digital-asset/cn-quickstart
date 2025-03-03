package com.digitalasset.quickstart.repository;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConfigurationProperties(prefix = "application")
public class TenantPropertiesRepository {

    private Map<String, TenantProperties> tenants = new ConcurrentHashMap<>();

    public static class TenantProperties {
        private String tenantId;
        private boolean internal;
        private String partyId;
        private String walletUrl;

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getPartyId() {
            return partyId;
        }

        public void setPartyId(String partyId) {
            this.partyId = partyId;
        }

        public String getWalletUrl() {
            return walletUrl;
        }

        public void setWalletUrl(String walletUrl) {
            this.walletUrl = walletUrl;
        }


        public boolean isInternal() {
            return internal;
        }

        public void setInternal(boolean internal) {
            this.internal = internal;
        }
    }

    /**
     * Spring will automatically bind the YAML 'application.tenants.*' to this map.
     */
    public Map<String, TenantProperties> getAllTenants() {
        return tenants;
    }

    /**
     * Called by Spring at context startup to set the initial map from YAML
     */
    public void setTenants(Map<String, TenantProperties> tenants) {
        this.tenants = new ConcurrentHashMap<>(tenants);
    }

    /**
     * Retrieve a single tenant's extra properties (like walletUrl).
     */
    public TenantProperties getTenant(String tenantId) {
        return tenants.get(tenantId);
    }

    /**
     * Save (or overwrite) a tenant's extra properties.
     * Called when we create a new tenant registration at runtime, etc.
     */
    public void addTenant(String tenantId, TenantProperties props) {
        tenants.put(tenantId, props);
    }

    /**
     * Remove a tenant’s extra properties
     */
    public void removeTenant(String tenantId) {
        tenants.remove(tenantId);
    }
}
