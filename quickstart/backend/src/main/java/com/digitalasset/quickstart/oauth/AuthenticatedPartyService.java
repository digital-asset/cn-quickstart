// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.oauth;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthenticatedPartyService {

    public Optional<String> getParty() {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof OAuth2AuthenticationToken auth) ||
            !auth.isAuthenticated()
        ) {
            return Optional.empty();
        }

        return Optional.ofNullable(auth.getPrincipal().getAttribute("party"));
    }

    public String getPartyOrFail() {
        return getParty().orElseThrow(() -> new IllegalStateException("No authenticated party"));
    }
}
