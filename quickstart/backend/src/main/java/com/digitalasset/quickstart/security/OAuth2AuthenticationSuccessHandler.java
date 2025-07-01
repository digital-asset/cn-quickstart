// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.security;

import com.digitalasset.quickstart.config.SecurityConfig;
import com.digitalasset.quickstart.repository.TenantPropertiesRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Profile("oauth2")
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final TenantPropertiesRepository tenantPropertiesRepository;
    private final SecurityConfig securityConfig;

    public OAuth2AuthenticationSuccessHandler(OAuth2AuthorizedClientService authorizedClientService, TenantPropertiesRepository tenantPropertiesRepository, SecurityConfig securityConfig) {
        this.authorizedClientService = authorizedClientService;
        this.tenantPropertiesRepository = tenantPropertiesRepository;
        this.securityConfig = securityConfig;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken auth))
            throw new IllegalArgumentException("Authentication must be an instance of OAuth2AuthenticationToken");

        if (!(auth.getPrincipal() instanceof OidcUser oidcUser))
            throw new IllegalArgumentException("Authentication Principal must be an instance of OidcUser");

        ClientRegistration clientReg = authorizedClientService.loadAuthorizedClient(auth.getAuthorizedClientRegistrationId(), auth.getName()).getClientRegistration();

        List<GrantedAuthority> authorities = new ArrayList<>();
        if (securityConfig.getIssuerUrl().equals(clientReg.getProviderDetails().getIssuerUri())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        Map<String, Object> claimsWithParty = new HashMap<>(oidcUser.getClaims());
        claimsWithParty.put(AuthService.VIRTUAL_TENANT_ID_CLAIM, clientReg.getClientName());
        claimsWithParty.put(AuthService.VIRTUAL_PARTY_ID_CLAIM, tenantPropertiesRepository.getTenant(clientReg.getClientName()).getPartyId());

        OidcIdToken idTokenWithParty = new OidcIdToken(oidcUser.getIdToken().getTokenValue(), oidcUser.getIssuedAt(), oidcUser.getExpiresAt(), claimsWithParty);

        OAuth2AuthenticationToken newAuth = new OAuth2AuthenticationToken(
                new DefaultOidcUser(authorities, idTokenWithParty, oidcUser.getUserInfo()),
                authorities,
                auth.getAuthorizedClientRegistrationId()
        );
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        // workaround spring security bug that doesn't set csrf token on oauth2 success
        // see https://github.com/spring-projects/spring-security/issues/12141#issuecomment-1321215874
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        csrfToken.getToken();
        SavedRequestAwareAuthenticationSuccessHandler handler = new SavedRequestAwareAuthenticationSuccessHandler();
        handler.onAuthenticationSuccess(request, response, newAuth);
    }
}
