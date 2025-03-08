package com.digitalasset.quickstart.security;

public interface TokenProvider {
    /**
     * Get the JWT token for backend channels.
     */
    String getToken();
}
