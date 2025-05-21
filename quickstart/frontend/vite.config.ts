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
