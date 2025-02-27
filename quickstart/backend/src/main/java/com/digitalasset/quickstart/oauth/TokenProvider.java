package com.digitalasset.quickstart.oauth;

public interface TokenProvider {
    /**
     * Get the JWT token for backend channels.
     */
    String getToken();
}
