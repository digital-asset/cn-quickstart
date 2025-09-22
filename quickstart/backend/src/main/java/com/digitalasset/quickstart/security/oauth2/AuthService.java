package com.digitalasset.quickstart.security.oauth2;

import com.digitalasset.quickstart.security.*;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Profile("oauth2")
final class AuthService implements AuthenticatedPartyProvider, AuthenticatedUserProvider, TokenProvider {

    private final OAuth2AuthorizedClientManager authorizedClientManager;
    static final String CLIENT_REGISTRATION_ID = "AppProviderBackend";

    AuthService(OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    @Override
    public Optional<String> getParty() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!auth.isAuthenticated() ||
            !(auth instanceof OAuth2AuthenticationToken) &&
            !(auth instanceof JwtAuthenticationToken)
        ) {
            return Optional.empty();
        }

        return auth.getAuthorities().stream().filter(PartyAuthority.class::isInstance).map(GrantedAuthority::getAuthority).findFirst();
    }

    @Override
    public String getPartyOrFail() {
        return getParty().orElseThrow(() -> new IllegalStateException("No authenticated party"));
    }

    @Override
    public String getToken() {
        OAuth2AuthorizeRequest req = OAuth2AuthorizeRequest.withClientRegistrationId(CLIENT_REGISTRATION_ID).principal("N/A").build();
        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(req);
        assert authorizedClient != null;
        return authorizedClient.getAccessToken().getTokenValue();
    }

    @Override
    public Optional<AuthenticatedUser> getUser() {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof OAuth2AuthenticationToken auth) ||
                !auth.isAuthenticated()
        ) {
            return Optional.empty();
        }

        // Extract user and role info
        List<String> authorities = auth.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        var partyId = auth.getAuthorities().stream()
                .filter(PartyAuthority.class::isInstance)
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No party authority found"));

        var tenantId = auth.getAuthorities().stream()
                .filter(TenantAuthority.class::isInstance)
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No tenant authority found"));

        return Optional.of(new DefaultAuthenticatedUser(
                auth.getPrincipal().getAttribute("name"),
                partyId,
                tenantId,
                authorities,
                authorities.contains("ROLE_ADMIN")
        ));
    }
}
