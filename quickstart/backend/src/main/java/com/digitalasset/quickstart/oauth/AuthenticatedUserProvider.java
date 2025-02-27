package com.digitalasset.quickstart.oauth;

import java.util.List;
import java.util.Optional;

public interface AuthenticatedUserProvider {
    /**
     * Get the authenticated user, if any.
     * @return
     */
    Optional<AuthenticatedUser> getUser();

    class AuthenticatedUser {
        String username;
        String partyId;
        String tenantId;
        List<String> roles;
        Boolean isAdmin;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPartyId() {
            return partyId;
        }

        public void setPartyId(String partyId) {
            this.partyId = partyId;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }

        public Boolean isAdmin() {
            return isAdmin;
        }

        public void setAdmin(Boolean admin) {
            isAdmin = admin;
        }
    }
}

