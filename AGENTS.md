# AGENTS.md

Guidance for AI coding agents working with this repository.

## Documentation

Use the Canton Network documentation at https://docs.canton.network:

- [llms.txt](https://docs.canton.network/llms.txt): index of the docs. Append `.md` to any page URL to get its Markdown.
- [MCP server](https://docs.canton.network/shared/mcp-server): search and read the docs from an MCP-capable client.
- [Version Compatibility Dashboard](https://docs.canton.network/shared/version-compatibility-dashboard): component versions running on DevNet, TestNet and MainNet.

Don't use Daml 2.x documentation (docs.daml.com) or the old `/build`, `/operate` and `/overview` pages on docs.digitalasset.com. Canton 3.x commands, APIs and configuration differ.

## Choose the path for your task

All `make` commands run from the `quickstart/` directory.

**To deploy and test your own Daml code**, you only need the network. Start LocalNet without building the Quickstart application:

```bash
cd quickstart
make start-localnet   # needs only Docker
make status-localnet
make clean-localnet   # stop and remove data when done
```

Don't run `make setup`, `make build` or `make start` for this. `make start` always runs `make build`, which builds the Java backend, the React frontend and the Daml model.

**To study or extend the Quickstart application** (the licensing demo with a Java backend and React frontend), follow the [Quickstart installation guide](https://docs.canton.network/appdev/quickstart/prerequisites): `make setup`, `make build`, `make start`.

## LocalNet facts

- Three validators: App User (port prefix `2`), App Provider (`3`) and Super Validator (`4`). The Super Validator also runs the synchronizer.
- JSON Ledger API: `http://localhost:2975`, `:3975`, `:4975`. gRPC Ledger API: `:2901`, `:3901`, `:4901`.
- Readiness: `curl -f http://localhost:3975/readyz` (JSON Ledger API) and `curl -f http://localhost:3903/api/validator/readyz` (validator).
- Authentication without OAuth2 (`make start-localnet`, or `make setup` with OAuth2 off): JWT signed with HS256, secret `unsafe`, audience `https://canton.network.global`, subject `ledger-api-user`. The env files write the audience as `${AUTH_..._AUDIENCE:-https://canton.network.global}`; use the resolved URL.
- `ledger-api-user` is an admin, but admin rights don't allow submitting or reading as a party. Allocate parties with `"userId": "ledger-api-user"` in `POST /v2/parties` to grant `CanActAs`, or the requests fail with HTTP 403.
- If a port is already in use, `docker compose up` fails with `address already in use`. Set `DB_PORT` when PostgreSQL already runs on 5432.

Details: [LocalNet](https://docs.canton.network/sdks-tools/development-tools/localnet).

## Common Daml and Ledger API pitfalls

- Define Daml interfaces in their own package, separate from the templates that implement them, and build both with a `multi-package.yaml` and `dpm build --all`. A package that defines and implements an interface fails to compile. See [Interfaces](https://docs.canton.network/appdev/modules/m3-interfaces).
- Upload DARs with `POST /v2/dars?vetAllPackages=true` and `Content-Type: application/octet-stream`.
- In query filters (`TemplateFilter`, `InterfaceFilter`), write identifiers in package-name form, `#<package-name>:<Module>:<Entity>`. Responses return the package-ID form, which filters reject. See [Identifier Formats](https://docs.canton.network/sdks-tools/api-reference/json-api#identifier-formats).

## Versions

`quickstart/.env` pins `SPLICE_VERSION` (LocalNet images) and `DAML_RUNTIME_VERSION` (Daml libraries for the Java backend). The Daml projects set the same SDK version in their `daml.yaml` files. These pins can lag the shared networks; check the Version Compatibility Dashboard before deploying to DevNet, TestNet or MainNet.

## Changing this repository

- Build: `make build`. Daml tests: `make test`. Integration tests: `make integration-test`.
- Run `make help` for all targets.
- New source files carry the header used in existing files, for example in `quickstart/Makefile`.
