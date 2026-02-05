// Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.security.oauth2;

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Lazy(false)
@Profile("oauth2")
public class OAuth2ClientRegistrationRepository
        implements AuthClientRegistrationRepository, ClientRegistrationRepository, Iterable<ClientRegistration> {

    private final Map<String, ClientRegistration> registrations;


    public OAuth2ClientRegistrationRepository(OAuth2ClientProperties properties) {

        // Map Spring Boot's properties -> standard ClientRegistration
        List<ClientRegistration> baseRegistrations = new ArrayList<>(
                new OAuth2ClientPropertiesMapper(properties).asClientRegistrations().values()
        );

        // Build up the map
        this.registrations = baseRegistrations.stream()
                .collect(Collectors.toMap(
                                ClientRegistration::getRegistrationId,
                                registration -> registration
                        )
                );
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        return registrations.get(registrationId);
    }

    @Override
    public String registerClient(Client client) throws IllegalArgumentException {
        // Prevent duplicates: same combination clientId + issuerURL
        boolean exists =
            getClientRegistrations().stream()
                .anyMatch(c ->
                              c.getClientId().equals(client.getClientId()) &&
                                  c.getIssuerURL().equals(client.getIssuerURL()));
        if (exists) {
            throw new IllegalArgumentException(
                "Duplicate client registration not allowed for clientId=" + client.getClientId() +
                    " and issuerURL=" + client.getIssuerURL());
        }
        String registrationId = client.getTenantId() + "-" + client.getClientId();

        ClientRegistration registration = ClientRegistrations.fromIssuerLocation(client.getIssuerURL())
                                              .registrationId(registrationId)
                                              .clientId(client.getClientId())
                                              .clientName(client.getTenantId())
                                              .scope("openid")
                                              .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                                              .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                                              .build();

        // Prevent duplicates by registrationId
        ClientRegistration previous = registrations.putIfAbsent(registrationId, registration);
        if (previous != null) {
            throw new IllegalStateException("RegistrationId already exists: " + registrationId);
        }

        return registrationId;
    }

    @Override
    public void removeClientRegistration(String tenantId, String clientId) {
        String key = tenantId + "-" + clientId;
        if (registrations.remove(key) == null) {
            throw new NoSuchElementException("No registration found for tenantId=" + tenantId + " clientId=" + clientId);
        }
    }

    public void removeClientRegistrations(String tenantId) {
        List<String> keysToRemove = registrations.values().stream()
                .filter(clientRegistration -> clientRegistration.getClientName().equals(tenantId))
                .map(ClientRegistration::getRegistrationId)
                .toList();
        if (keysToRemove.isEmpty()) {
            throw new NoSuchElementException("No registrations found for tenantId=" + tenantId);
        }
        keysToRemove.forEach(registrations::remove);
    }

    @Override
    public Collection<Client> getClientRegistrations() {
        return registrations.values().stream().filter(r ->
                r.getAuthorizationGrantType().equals(AuthorizationGrantType.AUTHORIZATION_CODE)
        ).map(r -> {
                    Client c = new Client();
                    c.setRegistrationId(r.getRegistrationId());
                    c.setTenantId(r.getClientName());
                    c.setClientId(r.getClientId());
                    c.setIssuerURL(r.getProviderDetails().getIssuerUri());
                    return c;
                }
        ).toList();
    }

    @Override
    public String getLoginLink(String clientRegistrationId) {
        return "/oauth2/authorization/" + clientRegistrationId;
    }

    @Override
    public Iterator<ClientRegistration> iterator() {
        return registrations.values().iterator();
    }
}
