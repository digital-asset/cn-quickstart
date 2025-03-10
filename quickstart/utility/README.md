# Canton Network Utilities Integration

Integration to bring the Canton Network Utilities into the quickstart application. 

Link to the Utility documentation:
- [Utilities docs](https://docs.utility.canton.network.digitalasset.com/doc/daml-api/html/index.html)

### Artifactory Access

On top of the artifactory access required for QS ensure you have access to Digital Assets's Utilities.

```bash
docker login digitalasset-canton-network-utility-docker.jfrog.io  -u "<user_name>" -p "<user_password>"
```

#### TODO - move below to pull creds inline with rest of QS
Currently an `envrc.pivate` file is required within the utility folder with the format:

```bash
export ARTIFACTORY_USER=<user_name>
export ARTIFACTORY_PASSWORD=<password>
```

## Utilities

To run the utilities:  

```bash
# Fetch the required utility dars
$ ./scripts/get-utility-dars.sh 

# Build the utility bootstrap model
$ daml build 

# Run the Utility
$ ./scripts/bootstrap-utility.sh

```

#### Default Utility parties created and their associated participant
- utility-operator (provider-participant)
- utility-provider (user-participant)
- utility-registrar (user-participant)
- utility-issuer (user-participant)
- utility-holder-1 (user-participant)

### Utility UI 

- **Application User Frontend**
  - **URL**: [http://localhost:8000](http://localhost:8000)

### Current outstanding issues

- `utility-credential-v0-0.0.2.dar` is not part of the utility dar bundle therefore needs to be hardcoded into the project 

- utility ui does not provide `openid` in its scope when logging in. This breaks the mockOauth. Current workaround is to manually edit the url. There must be a better way?? 

- Build in/document a way to simulate the utility backend automation