import type {APIRequestContext} from 'playwright-core';

export async function createParty(request: APIRequestContext, token: string, partyIdHint: string, participant: string): Promise<string> {
  const namespace = await getParticipantNamespace(request, token, participant);
  const parties = await request.get(
    `http://${participant}/v2/parties/party?parties=${partyIdHint}::${namespace}`,
    {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    }
  );
  if (!parties.ok()) {
    throw new Error(`GET request failed with status ${parties.status()}`);
  }
  const partiesJson = await parties.json();
  const party: string = partiesJson.partyDetails?.[0]?.party;
  if (party && party !== 'null') {
    console.error(`party exists ${party}`);
    return party;
  }

  const newParty = await request.post(
    `http://${participant}/v2/parties`,
    {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      data: {
        partyIdHint,
        displayName: partyIdHint,
        identityProviderId: ""
      }
    }
  );
  if (!newParty.ok()) {
    throw new Error(`POST request failed with status ${newParty.status()}`);
  }
  const newPartyJson = await newParty.json();
  const partyId: string = newPartyJson.partyDetails?.party;
  console.log(`Ledger party created with id: ${partyId}`);
  return partyId;
}

export async function getParticipantNamespace(request: APIRequestContext, token: string, participant: string): Promise<string> {
  const response = await request.get(`http://${participant}/v2/parties/participant-id`, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });
  if (!response.ok()) {
    throw new Error(`Request failed with status ${response.status()}`);
  }
  const data = await response.json();
  const participantId: string = data.participantId;
  return participantId.replace(/^participant::/, '');
}

export async function createUser(
  request: APIRequestContext,
  token: string,
  userId: string,
  userName: string,
  party: string,
  participant: string,
) {
  const baseUrl = `http://${participant}`

  // 1) check if user exists
  const check = await request.get(`${baseUrl}/v2/users/${userId}`, {
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  })

  if (check.status() === 404) {
    // 2) create the user
    const create = await request.post(`${baseUrl}/v2/users`, {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      data: {
        user: {
          id: userId,
          isDeactivated: false,
          primaryParty: party,
          identityProviderId: '',
          metadata: {
            resourceVersion: '',
            annotations: {username: userName},
          },
        },
        rights: [],
      },
    })

    if (!create.ok()) {
      throw new Error(
        `Failed to create user ${userId}: ${create.status()} ${await create.text()}`
      )
    }

    const json = await create.json()
    console.log(`Ledger user created with id: ${json.user.id} for party: ${json.user.primaryParty}`);
    return json;
  }

  return check.json();
}


export async function grantRights(
  request: APIRequestContext,
  token: string,
  userId: string,
  partyId: string,
  rightsCsv: string,
  participant: string,
) {
  console.log(`Grant rights user:${userId} party:${partyId}`)

  const rights = rightsCsv.split(' ').map(r => {
    switch (r) {
      case 'ParticipantAdmin':
        return {kind: {ParticipantAdmin: {value: {}}}}
      case 'ActAs':
        return {kind: {CanActAs: {value: {party: partyId}}}}
      case 'ReadAs':
        return {kind: {CanReadAs: {value: {party: partyId}}}}
      default:
        throw new Error(`Unknown right: ${r}`)
    }
  })

  const response = await request.post(`http://${participant}/v2/users/${userId}/rights`, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    data: {
      userId,
      identityProviderId: '',
      rights,
    },
  })

  if (response.status() !== 200) {
    const body = await response.text()
    throw new Error(`grantRights failed ${response.status()}: ${body}`)
  }

  return response.json()
}
