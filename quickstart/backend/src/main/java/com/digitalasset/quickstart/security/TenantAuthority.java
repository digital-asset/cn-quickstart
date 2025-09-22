package com.digitalasset.quickstart.security;

import org.springframework.security.core.GrantedAuthority;

public class TenantAuthority implements GrantedAuthority {
    private final String tentant;

    public TenantAuthority(String tentant) {
        this.tentant = tentant;
    }
    @Override
    public String getAuthority() {
        return tentant;
    }
}
