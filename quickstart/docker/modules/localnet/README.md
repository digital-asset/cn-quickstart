# run as localnet
```
docker compose --env-file docker/modules/localnet/compose.env \
               --env-file docker/modules/localnet/env/common.env \
               --env-file docker/modules/localnet/env/localnet.env \
               -f docker/modules/localnet/compose.yaml \
               -f docker/modules/localnet/resource-constraints.yaml \
               --profile localnet up -d
```
# run as devnet
```
docker compose --env-file docker/modules/localnet/compose.env \
               --env-file docker/modules/localnet/env/common.env \
               --env-file docker/modules/localnet/env/devnet.env \
               -f docker/modules/localnet/compose.yaml \
               -f docker/modules/localnet/resource-constraints.yaml \
               up -d
```
