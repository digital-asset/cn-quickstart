package com.digitalasset.quickstart.security.oauth2;

import java.util.Collection;

public interface AuthClientRegistrationRepository {

    String registerClient(Client client) throws IllegalArgumentException;
    void removeClientRegistration(String tenantId, String clientId);
    void removeClientRegistrations(String tenantId);
    Collection<Client> getClientRegistrations();
    String getLoginLink(String clientRegistrationId);

    class Client {
        private String registrationId;
        private String tenantId;
        private String clientId;
        private String issuerURL;

        public String getRegistrationId() {
            return registrationId;
        }

        public void setRegistrationId(String registrationId) {
            this.registrationId = registrationId;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getIssuerURL() {
            return issuerURL;
        }

        public void setIssuerURL(String issuerURL) {
            this.issuerURL = issuerURL;
        }
    }
}
