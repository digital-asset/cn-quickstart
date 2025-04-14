package com.digitalasset.quickstart.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
final class AuthService implements AuthenticatedPartyProvider, AuthenticatedUserProvider, TokenProvider {

  private final OAuth2AuthorizedClientManager authorizedClientManager;
  static final String CLIENT_REGISTRATION_ID = "AppProviderBackend";
  static final String VIRTUAL_PARTY_ID_CLAIM = "virtual_partyId";
  static final String VIRTUAL_TENANT_ID_CLAIM = "virtual_tenantId";

  AuthService(OAuth2AuthorizedClientManager authorizedClientManager) {
    this.authorizedClientManager = authorizedClientManager;
  }

  @Override
  public Optional<String> getParty() {
    if (!(SecurityContextHolder.getContext().getAuthentication() instanceof OAuth2AuthenticationToken auth) ||
            !auth.isAuthenticated()
    ) {
      return Optional.empty();
    }

    return Optional.ofNullable(auth.getPrincipal().getAttribute(AuthService.VIRTUAL_PARTY_ID_CLAIM));
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

    AuthenticatedUser user = new AuthenticatedUser();
    user.setUsername(auth.getPrincipal().getAttribute("name"));
    user.setPartyId(auth.getPrincipal().getAttribute(VIRTUAL_PARTY_ID_CLAIM));
    user.setTenantId(auth.getPrincipal().getAttribute(VIRTUAL_TENANT_ID_CLAIM));
    user.setRoles(authorities);
    user.setAdmin(authorities.contains("ROLE_ADMIN"));
    return Optional.of(user);
  }
}
