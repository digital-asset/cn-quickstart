import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import ViteYaml from '@modyfi/vite-plugin-yaml'

const portFromEnv = process.env.BACKEND_PORT || 8080;

function setProxyCustomHeaders(proxy: any) {
    proxy.on('proxyReq', (proxyReq: any, req: any) => {
        // Set custom headers similar to nginx's proxy_set_header
        proxyReq.setHeader('Content-Type', req.headers['content-type'] || '')
        proxyReq.setHeader('X-Real-IP', req.socket.remoteAddress || '')
        proxyReq.setHeader('X-Forwarded-Host', req.headers['host'] || '')
        proxyReq.setHeader('X-Forwarded-For', req.headers['x-forwarded-for'] || req.socket.remoteAddress || '')
        proxyReq.setHeader('X-Forwarded-Proto', 'http')
        proxyReq.setHeader('X-Forwarded-Port', 5173)
    });
}

export default defineConfig({
    plugins: [
        react(),
        ViteYaml(),
    ],
    server: {
        host: 'app-provider.localhost',
        proxy: {
            '/api': {
                target: `http://localhost:${portFromEnv}/`,
                changeOrigin: false,
                rewrite: (path) => path.replace(/^\/api/, ''),
                configure: setProxyCustomHeaders
           },
            '/login': {
                target: `http://localhost:${portFromEnv}/`,
                changeOrigin: false,
                configure: setProxyCustomHeaders
            },
            '/login/oauth2': {
                target: `http://localhost:${portFromEnv}/`,
                changeOrigin: false,
                configure: setProxyCustomHeaders
            },
            '/oauth2': {
                target: `http://localhost:${portFromEnv}/` ,
                changeOrigin: false,
                configure: setProxyCustomHeaders
            },
        },
    },
})
