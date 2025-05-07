# run in localnet
## start
```
docker compose --env-file $LOCALNET_DIR/compose.env \
               --env-file $LOCALNET_DIR/env/common.env \
               --env-file $LOCALNET_DIR/env/local.env \
               -f $LOCALNET_DIR/compose.yaml \
               -f $LOCALNET_DIR/resource-constraints.yaml \
               --profile sv \
               --profile app-provider \
               --profile app-user up -d
```
## stop
```
docker compose --env-file $LOCALNET_DIR/compose.env \
               --env-file $LOCALNET_DIR/env/common.env \
               --env-file $LOCALNET_DIR/env/local.env \
               -f $LOCALNET_DIR/compose.yaml \
               -f $LOCALNET_DIR/resource-constraints.yaml \
               --profile sv \
               --profile app-provider \
               --profile app-user down -v
```

## console
```
docker compose --env-file $LOCALNET_DIR/compose.env \
               --env-file $LOCALNET_DIR/env/common.env \
               --env-file $LOCALNET_DIR/env/local.env \
               -f $LOCALNET_DIR/compose.yaml \
               -f $LOCALNET_DIR/resource-constraints.yaml \
               run --rm console
```

# run in localnet with keycloak

```
docker compose --env-file $LOCALNET_DIR/compose.env \
               --env-file $LOCALNET_DIR/env/common.env \
               --env-file $MODULES_DIR/keycloak/compose.env \
               --env-file $LOCALNET_DIR/env/local.env \
               -f $LOCALNET_DIR/compose.yaml \
               -f $LOCALNET_DIR/resource-constraints.yaml \
               -f $MODULES_DIR/keycloak/compose.yaml \
               --profile sv \
               --profile app-provider \
               --profile app-user \
               --profile oauth2 up -d
```
## console
```
docker compose --env-file $LOCALNET_DIR/compose.env \
               --env-file $LOCALNET_DIR/env/common.env \
               --env-file $MODULES_DIR/keycloak/compose.env \
               --env-file $LOCALNET_DIR/env/local.env \
               -f $LOCALNET_DIR/compose.yaml \
               -f $LOCALNET_DIR/resource-constraints.yaml \
               -f $MODULES_DIR/keycloak/compose.yaml \
               --profile sv \
               --profile app-provider \
               --profile app-user \
               --profile oauth2 \
               run --rm console
```

# run in devnet
```
docker compose --env-file ${LOCALNET_DIR}/compose.env \
               --env-file ${LOCALNET_DIR}/env/common.env \
               --env-file ${LOCALNET_DIR}/env/dev.env \
               -f ${LOCALNET_DIR}/compose.yaml \
               -f ${LOCALNET_DIR}/resource-constraints.yaml \
               up -d
```
