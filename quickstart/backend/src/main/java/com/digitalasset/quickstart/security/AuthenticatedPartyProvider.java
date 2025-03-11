package com.digitalasset.quickstart.security;

import java.util.Optional;

public interface AuthenticatedPartyProvider {
    /**
     * Get the party ID of the authenticated user, if any.
     * @return the party ID, or empty if no user is authenticated.
     */
    Optional<String> getParty();

    /**
     * Get the party ID of the authenticated user, or throw an exception if no user is authenticated.
     * @return the party ID
     * @throws IllegalStateException if no user is authenticated
     */
    String getPartyOrFail();
}
