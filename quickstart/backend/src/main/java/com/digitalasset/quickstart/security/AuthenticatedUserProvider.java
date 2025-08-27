package com.digitalasset.quickstart.security;

import java.util.List;
import java.util.Optional;

public interface AuthenticatedUserProvider {
    /**
     * Get the authenticated user, if any.
     * @return
     */
    Optional<AuthenticatedUser> getUser();

    final class AuthenticatedUser {
        String username;
        String partyId;
        String tenantId;
        List<String> roles;
        Boolean isAdmin;

        AuthenticatedUser() {}

        public String getUsername() {
            return username;
        }

        void setUsername(String username) {
            this.username = username;
        }

        public String getPartyId() {
            return partyId;
        }

        void setPartyId(String partyId) {
            this.partyId = partyId;
        }

        public String getTenantId() {
            return tenantId;
        }

        void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public List<String> getRoles() {
            return roles;
        }

        void setRoles(List<String> roles) {
            this.roles = roles;
        }

        public Boolean isAdmin() {
            return isAdmin;
        }

        void setAdmin(Boolean admin) {
            isAdmin = admin;
        }
    }
}

