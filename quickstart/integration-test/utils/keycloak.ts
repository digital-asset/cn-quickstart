import type {APIRequestContext} from 'playwright-core';

const KEYCLOAK_HOST =  process.env.KEYCLOAK_HOST!;

export async function createUser(request: APIRequestContext, partyId: string, tag: string): Promise<string> {
  const accessToken = await getKeycloakAdminToken(request);
  const createUserResponse = await request.post(`${KEYCLOAK_HOST}/admin/realms/AppUser/users`, {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${accessToken}`,
    },
    data: {
      username: `app-user-${tag}`,
      email: `app-user-${tag}@app-user.localhost`,
      firstName: 'app',
      lastName: `user ${tag}`,
      enabled: true,
      attributes: {
        partyId: [partyId],
      },
      credentials: [
        {
          type: 'password',
          value: 'abc123',
          temporary: false,
        },
      ],
    },
  });

  if (!createUserResponse.ok()) {
    throw new Error(`Failed to create user: ${createUserResponse.status()} ${await createUserResponse.text()}`);
  }

  // 4. Extract the new user ID from the Location header
  const locationHeader = createUserResponse.headers()['location'];
  if (!locationHeader) {
    throw new Error('Location header missing in create user response');
  }
  const parts = locationHeader.split('/');
  const userId = parts[parts.length - 1];
  if (!userId) {
    throw new Error('Location header does not contain a valid userId');
  }

  console.log(`Keycloak user created with id: ${userId}`);
  return userId;
}

export async function getKeycloakAdminToken(request: APIRequestContext): Promise<string> {

  const tokenResponse = await request.post(
      `${KEYCLOAK_HOST}/realms/master/protocol/openid-connect/token`,
      {
      headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
      },
      form: {
          client_id: 'admin-cli',
          grant_type: 'password',
          username: 'admin',
          password: 'admin',
      },
      }
  );
  if (!tokenResponse.ok()) {
      throw new Error(
      `Failed to fetch keycloak admin token: ${tokenResponse.status()} ${await tokenResponse.text()}`
      );
  }

  const { access_token: accessToken } = await tokenResponse.json();
  return accessToken;
}

/**
 * Fetches an access token using the client_credentials grant.
 */
export async function getAdminToken(
  request: APIRequestContext,
  clientSecret: string,
  clientId: string,
  tokenUrl: string
): Promise<string> {
  console.log(`Get Admin Token ${clientId}`)
  const response = await request.post(`${KEYCLOAK_HOST}/realms/AppUser/protocol/openid-connect/token`, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    form: {
      client_id: clientId,
      client_secret: clientSecret,
      grant_type: 'client_credentials',
      scope: 'openid',
    },
  })

  if (!response.ok()) {
    throw new Error(
      `Failed to fetch admin token: ${response.status()} ${await response.text()}`
    )
  }

  const { access_token: accessToken } = await response.json()
  return accessToken
}


export async function getUserToken(
    request: APIRequestContext,
    username: string,
    password: string,
    clientId: string
): Promise<string> {
    const response = await request.post(`${KEYCLOAK_HOST}/realms/AppUser/protocol/openid-connect/token`, {
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        form: {
            client_id: clientId,
            username,
            password,
            grant_type: 'password',
            scope: 'openid',
        },
    })

    if (!response.ok()) {
        throw new Error(
            `Failed to fetch user token: ${response.status()} ${await response.text()}`
        )
    }

    const { access_token: accessToken } = await response.json()
    return accessToken
}
