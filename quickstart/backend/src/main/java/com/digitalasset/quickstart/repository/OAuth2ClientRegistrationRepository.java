// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.repository;

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Lazy(false)
public class OAuth2ClientRegistrationRepository
        implements ClientRegistrationRepository, Iterable<ClientRegistration> {

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
                        registration -> {
                            Map<String, Object> metadata = new HashMap<>(registration.getProviderDetails().getConfigurationMetadata());
                            metadata.put("preconfigured", "true");
                            return ClientRegistration.withRegistrationId(registration.getRegistrationId())
                                    .clientId(registration.getClientId())
                                    .clientSecret(registration.getClientSecret())
                                    .authorizationUri(registration.getProviderDetails().getAuthorizationUri())
                                    .authorizationGrantType(registration.getAuthorizationGrantType())
                                    .tokenUri(registration.getProviderDetails().getTokenUri())
                                    .jwkSetUri(registration.getProviderDetails().getJwkSetUri())
                                    .issuerUri(registration.getProviderDetails().getIssuerUri())
                                    .redirectUri(registration.getRedirectUri())
                                    .scope(registration.getScopes())
                                    .clientName(registration.getClientName())
                                    .providerConfigurationMetadata(metadata)
                                    .build();
                        }
                ));
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        return registrations.get(registrationId);
    }

    public void addRegistration(ClientRegistration registration) {
        registrations.put(registration.getRegistrationId(), registration);
    }

    public void removeRegistration(String registrationId) {
        registrations.remove(registrationId);
    }

    public Collection<ClientRegistration> getRegistrations() {
        return registrations.values();
    }

    @Override
    public Iterator<ClientRegistration> iterator() {
        return registrations.values().iterator();
    }
}


