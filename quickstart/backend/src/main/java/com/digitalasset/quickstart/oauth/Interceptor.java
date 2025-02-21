// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.oauth;

import io.grpc.*;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;

@Component
public class Interceptor implements ClientInterceptor {

    @Autowired
    private OAuth2AuthorizedClientManager authorizedClientManager;
    private final Metadata.Key<String> AUTHORIZATION_HEADER = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
    private final String CLIENT_REGISTRATION_ID = "AppProviderBackend-Participant";

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        OAuth2AuthorizeRequest req = OAuth2AuthorizeRequest.withClientRegistrationId(CLIENT_REGISTRATION_ID).principal("N/A").build();
        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(req);
        assert authorizedClient != null;
        String accessToken = authorizedClient.getAccessToken().getTokenValue();

        ClientCall<ReqT, RespT> clientCall = next.newCall(method, callOptions);
        return new SimpleForwardingClientCall<ReqT, RespT>(clientCall) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(AUTHORIZATION_HEADER, "Bearer " + accessToken);
                super.start(responseListener, headers);
            }
        };
    }
}

