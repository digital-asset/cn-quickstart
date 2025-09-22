package com.digitalasset.quickstart.security;

import org.springframework.security.core.GrantedAuthority;

public class PartyAuthority implements GrantedAuthority {

    private final String party;

    public PartyAuthority(String party) {
        this.party = party;
    }
    @Override
    public String getAuthority() {
        return party;
    }
}
