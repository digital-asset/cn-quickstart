import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import ViteYaml from '@modyfi/vite-plugin-yaml'

const portFromEnv = process.env.APP_PROVIDER_UI_PORT || '3000'

export default defineConfig({
    plugins: [
        react(),
        ViteYaml(),
    ],
    server: {
        host: true,
        // This port needs to match the port of the oauth redirectUris. This is normally APP_PROVIDER_UI_PORT, but
        // vite puts a reverse proxy in front of the app, so we need to set APP_PROVIDER_UI_PORT to something else
        // and then reverse proxy to APP_PROVIDER_UI_PORT on the port that the redirectUris expect.
        port: 3000,
        proxy: {
            '/api': {
                target: `http://localhost:${portFromEnv}/`,
                changeOrigin: false,
            },
            '/login/oauth2': {
                target: `http://localhost:${portFromEnv}/`,
                changeOrigin: false,
            },
            '/oauth2': {
                target: `http://localhost:${portFromEnv}/`,
                changeOrigin: false,
            },
        },
    },
})
