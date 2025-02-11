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
        private String walletUrl;

        public String getWalletUrl() {
            return walletUrl;
        }

        public void setWalletUrl(String walletUrl) {
            this.walletUrl = walletUrl;
        }
    }

    /**
     * Spring will automatically bind the YAML 'tenants.*' to this map.
     */
    public Map<String, TenantProperties> getTenants() {
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
    public TenantProperties getProperties(String registrationId) {
        return tenants.get(registrationId);
    }

    /**
     * Save (or overwrite) a tenant's extra properties.
     * Called when we create a new tenant registration at runtime, etc.
     */
    public void saveProperties(String registrationId, TenantProperties props) {
        tenants.put(registrationId, props);
    }

    /**
     * Remove a tenant’s extra properties
     */
    public void removeProperties(String registrationId) {
        tenants.remove(registrationId);
    }
}
