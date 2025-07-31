import type {APIRequestContext} from 'playwright-core'
import {request} from 'playwright-core'

export class Keycloak {
  private kcRequest: APIRequestContext

  public async init(): Promise<void> {
    this.kcRequest = await request.newContext({
      baseURL: process.env.KEYCLOAK_BASE_URL!,
      extraHTTPHeaders: {Host: 'keycloak.localhost'},
    })
  }

  public async createUser(partyId: string, tag: string): Promise<string> {
    const token = await this.getKeycloakAdminToken()
    const res = await this.kcRequest.post('/admin/realms/AppUser/users', {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      data: {
        username: `app-user-${tag}`,
        email: `app-user-${tag}@app-user.localhost`,
        firstName: 'app',
        lastName: `user ${tag}`,
        enabled: true,
        attributes: {partyId: [partyId]},
        credentials: [{type: 'password', value: 'abc123', temporary: false}],
      },
    })
    if (!res.ok()) {
      throw new Error(
        `Failed to create user: ${res.status()} ${await res.text()}`,
      )
    }
    const location = res.headers()['location']
    if (!location) throw new Error('Location header missing')
    const userId = location.split('/').pop()
    if (!userId) throw new Error('Location header does not contain a valid userId')
    console.log(`Keycloak user created with id: ${userId}`);
    return userId;
  }

  private async getKeycloakAdminToken(): Promise<string> {
    const res = await this.kcRequest.post(
      '/realms/master/protocol/openid-connect/token',
      {
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        form: {
          client_id: 'admin-cli',
          grant_type: 'password',
          username: 'admin',
          password: 'admin',
        },
      },
    )
    if (!res.ok()) {
      throw new Error(
        `Failed to fetch keycloak admin token: ${res.status()} ${await res.text()}`,
      )
    }
    const {access_token} = await res.json()
    return access_token
  }

  /**
   * Fetches an access token using the client_credentials grant.
   */
  public async getAdminToken(
    clientSecret: string,
    clientId: string,
  ): Promise<string> {
    console.log(`Get Admin Token ${clientId}`)
    const res = await this.kcRequest.post(
      '/realms/AppUser/protocol/openid-connect/token',
      {
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        form: {
          client_id: clientId,
          client_secret: clientSecret,
          grant_type: 'client_credentials',
          scope: 'openid',
        },
      },
    )
    if (!res.ok()) {
      throw new Error(
        `Failed to fetch admin token: ${res.status()} ${await res.text()}`,
      )
    }
    const {access_token} = await res.json()
    return access_token
  }

  public async getUserToken(
    username: string,
    password: string,
    clientId: string,
  ): Promise<string> {
    const res = await this.kcRequest.post(
      '/realms/AppUser/protocol/openid-connect/token',
      {
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        form: {
          client_id: clientId,
          grant_type: 'password',
          username,
          password,
          scope: 'openid',
        },
      },
    )
    if (!res.ok()) {
      throw new Error(
        `Failed to fetch user token: ${res.status()} ${await res.text()}`,
      )
    }
    const {access_token} = await res.json()
    return access_token
  }
}
