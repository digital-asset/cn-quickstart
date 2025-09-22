package com.digitalasset.quickstart.security;

import java.util.List;
import java.util.Optional;

public interface AuthenticatedUserProvider {
    /**
     * Get the authenticated user, if any.
     * @return
     */
    Optional<AuthenticatedUser> getUser();

    sealed interface AuthenticatedUser permits DefaultAuthenticatedUser {
        String username();
        String partyId();
        String tenantId();
        List<String> roles();
        Boolean isAdmin();
    }

    /**
     * Default implementation of AuthenticatedUser.
     * For finer-grained control over instantiation and visibility, consider using the Java Platform Module System (JPMS)
     * @param username
     * @param partyId
     * @param tenantId
     * @param roles
     * @param isAdmin
     */
    record DefaultAuthenticatedUser(
            String username,
            String partyId,
            String tenantId,
            List<String> roles,
            Boolean isAdmin
    ) implements AuthenticatedUser {}
}

