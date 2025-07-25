import type {APIRequestContext} from 'playwright-core';

export async function onboardWalletUser(
  request: APIRequestContext,
  adminToken: string,
  user: string,
  party: string,
  validatorHost: string
): Promise<void> {
  console.log(`Onboard wallet user ${user} ${party} ${validatorHost}`);
  const response = await request.post(
    `http://${validatorHost}/api/validator/v0/admin/users`,
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${adminToken}`,
      },
      data: {
        party_id: party,
        name: user,
      },
    }
  );

  if (!response.ok()) {
    throw new Error(
      `Failed to onboard wallet user: ${response.status()} ${await response.text()}`
    );
  }
}

