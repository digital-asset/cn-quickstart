import pg from 'pg'
import type { BackendConfig } from '../config.js'

export const buildPool = (cfg: BackendConfig): pg.Pool => new pg.Pool({
  host: cfg.postgres.host,
  port: cfg.postgres.port,
  database: cfg.postgres.database,
  user: cfg.postgres.user,
  password: cfg.postgres.password,
  max: 10
})
