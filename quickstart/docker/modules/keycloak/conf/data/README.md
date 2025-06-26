# Keycloak export
Keycloak imports configuration from `docker/oauth/data/*.json` files on startup. Those file were exported from Keycloak that had been configured in Keycloak Administration Console as described below using 
```sh
/opt/keycloak/bin/kc.sh export --dir=/tmp/export --realm AppProvider
/opt/keycloak/bin/kc.sh export --dir=/tmp/export --realm AppUser
```

## Setup via Keycloak Administration Console
In http://keycloak.localhost:8082/admin/master/console/#/master admin/admin setup
- two realms
  - AppProvider
  - AppUser

For each realm create a `client scope` >
  - Type: Default
  - Protocol: OpenID Connect

with a `mapper` by configuration `Audience` >
  - Included Custom Audience: https://canton.network.global

For each realm create clients:
  - AppProvider:
    - app-provider-backend-oidc:
      - Client authentication: off
      - Authentication flow: Standard Flow
      - Valid redirect URIs: http://app-provider.localhost:3000/*
      - Valid post logout redirect URIs: +
      - Web origins: * 
    - app-provider-unsafe:
      - Client authentication: off
      - Authentication flow: Direct access grant  
    - app-provider-validator:
      - Client authentication: on
      - Authentication flow: Service accounts roles
    - app-provider-backend:
        - Client authentication: on
        - Authentication flow: Service accounts roles
    - app-provider-pqs:
        - Client authentication: on
        - Authentication flow: Service accounts roles
  - AppUser:
      - app-user-wallet:
          - Client authentication: off
          - Authentication flow: Standard Flow
          - Valid redirect URIs: http://wallet.localhost:2000
          - Valid post logout redirect URIs: +
          - Web origins: *
      - app-provider-backend-oidc:
          - Client authentication: off
          - Authentication flow: Standard Flow
          - Valid redirect URIs: http://app-provider.localhost:3000/*
          - Valid post logout redirect URIs: +
          - Web origins: *
      - app-user-unsafe:
          - Client authentication: off
          - Authentication flow: Direct access grant
      - app-user-validator:
          - Client authentication: on
          - Authentication flow: Service accounts roles 

For each realm create users:    
  - app-provider
  - app-user

### NOTE: if you make changes to keycloak configuration don't forget to change also .env file in the root directory of the project

