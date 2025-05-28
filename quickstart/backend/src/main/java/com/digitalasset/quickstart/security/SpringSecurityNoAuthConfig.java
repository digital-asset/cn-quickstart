// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.security;

import com.digitalasset.quickstart.config.SecurityConfig;
import com.digitalasset.quickstart.repository.TenantPropertiesRepository;
import com.digitalasset.quickstart.security.AuthenticatedUserProvider.AuthenticatedUser;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Configuration
@EnableWebSecurity
@Profile("noauth")
public class SpringSecurityNoAuthConfig {

    private final TenantPropertiesRepository tenantPropertiesRepository;
    private final SecurityConfig securityConfig;

    public SpringSecurityNoAuthConfig(TenantPropertiesRepository tenantPropertiesRepository, SecurityConfig securityConfig) {
        this.tenantPropertiesRepository = tenantPropertiesRepository;
        this.securityConfig = securityConfig;
    }

    @Bean
    public Auth auth() {
        return Auth.NONE;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/login", "/user", "/login-links", "/feature-flags", "/error", "/oauth2/authorization/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/logout").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("Unauthorized" + authException.getMessage() + " " + authException.getCause());

                        })
                ).formLogin(form -> form.loginPage("/login").permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.sendRedirect("/");
                        })
                );

        return http.build();
    }

    @Bean
    public UserDetailsManager userDetailsManager() {
        var users = new ArrayList<UserDetails>();
        tenantPropertiesRepository.getAllTenants()
                .forEach((tenantId, props) -> {
                    props.getUsers()
                            .forEach(userId -> {
                                var userBuilder = User.withUsername(userId).password("{noop}");
                                if (props.isInternal())
                                    userBuilder.roles("ADMIN");
                                users.add(userBuilder.build());
                            });
                });
        return new InMemoryUserDetailsManager(users);
    }

    @Bean
    public AuthenticatedUserProvider authenticatedUserProvider() {
        return () -> {
            if (SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken) {
                return Optional.empty();
            } else {
                var name = SecurityContextHolder.getContext().getAuthentication().getName();
                AuthenticatedUser user = new AuthenticatedUser();
                user.setUsername(name);
                tenantPropertiesRepository.getAllTenants().values().stream().filter(tenant -> tenant.getUsers().contains(name)).findFirst()
                        .ifPresent(tenant -> {
                            user.setTenantId(tenant.getTenantId());
                            user.setPartyId(tenant.getPartyId());
                            if (tenant.isInternal()) {
                                user.setRoles(List.of("ROLE_ADMIN"));
                                user.setAdmin(true);
                                user.setUsername(user.getUsername() + " the provider");
                            } else {
                                user.setRoles(List.of("ROLE_USER"));
                                user.setUsername(user.getUsername() + " the user");
                            }
                        });
                return Optional.of(user);
            }
        };
    }

    @Bean
    public TokenProvider tokenProvider() {
        // KV remove dependency on auth0 as we don't need it anymore
        return () -> securityConfig.getToken();
    }

    @Bean
    public AuthenticatedPartyProvider authenticatedPartyProvider() {
        return new AuthenticatedPartyProvider() {
            @Override
            public Optional<String> getParty() {
                var auth = SecurityContextHolder.getContext().getAuthentication();
                if (!auth.isAuthenticated()) {
                    return Optional.empty();
                }
                return tenantPropertiesRepository
                        .getAllTenants()
                        .values()
                        .stream()
                        .filter(tenant -> tenant.getUsers().contains(auth.getName()))
                        .findFirst()
                        .map(TenantPropertiesRepository.TenantProperties::getPartyId);
            }

            @Override
            public String getPartyOrFail() {
                return getParty().orElseThrow(() -> new IllegalStateException("No authenticated party"));
            }
        };
    }
}