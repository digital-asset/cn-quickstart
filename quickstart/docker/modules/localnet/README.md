# run in localnet
```
docker compose --env-file ${LOCALNET_DIR}/compose.env \
               --env-file ${LOCALNET_DIR}/env/common.env \
               --env-file ${LOCALNET_DIR}/env/localnet.env \
               -f ${LOCALNET_DIR}/compose.yaml \
               -f ${LOCALNET_DIR}/resource-constraints.yaml \
               --profile localnet up -d
```
# run in devnet
```
docker compose --env-file ${LOCALNET_DIR}/compose.env \
               --env-file ${LOCALNET_DIR}/env/common.env \
               --env-file ${LOCALNET_DIR}/env/devnet.env \
               -f ${LOCALNET_DIR}/compose.yaml \
               -f ${LOCALNET_DIR}/resource-constraints.yaml \
               up -d
```
