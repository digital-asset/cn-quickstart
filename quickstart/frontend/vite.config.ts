import {ConfigEnv, defineConfig, Plugin, loadEnv} from 'vite'
import react from '@vitejs/plugin-react'
import ViteYaml from '@modyfi/vite-plugin-yaml'


function printWelcomeMessage(): Plugin {
  return {
    name: 'print-welcome-message',
    configureServer(server) {
      const print = () => server.config.logger.info(
        `  \x1b[97mVisit \x1b[96mhttp://app-provider.localhost\x1b[1;96m:${server.config.server.port}\x1b[0m\x1b[97m to start\x1b[0m`
      );
      const http = server.httpServer;
      if (http && (http.listening || (http as any)._handle)) {
        print();
      } else if (http) {
        http.once('listening', print);
      } else {
        // fallback for older vite/node combos
        setTimeout(print, 100);
      }
    },
  };
}

function setProxyCustomHeaders(proxy: any) {
    proxy.on('proxyReq', (proxyReq: any, req: any) => {
        // Set custom headers similar to nginx's proxy_set_header
        proxyReq.setHeader('Content-Type', req.headers['content-type'] || '')
        proxyReq.setHeader('X-Real-IP', req.socket.remoteAddress || '')
        proxyReq.setHeader('X-Forwarded-Host', req.headers['host'] || '')
        proxyReq.setHeader('X-Forwarded-For', req.headers['x-forwarded-for'] || req.socket.remoteAddress || '')
        proxyReq.setHeader('X-Forwarded-Proto', 'http')
        proxyReq.setHeader('X-Forwarded-Port', 5173)
        proxyReq.setHeader('host', 'app-provider.localhost')
    });
}

export default defineConfig(({ mode }: ConfigEnv) => {
    const env = loadEnv(mode, '../');
    const backendPort = env.VITE_BACKEND_PORT || 8080;
    return {
        plugins: [
            react(),
            ViteYaml(),
            printWelcomeMessage()
        ],
        server: {
            host: 'localhost',
            strictPort: true,
            allowedHosts: ['app-provider.localhost'],
            proxy: {
                '/api': {
                    target: `http://localhost:${backendPort}/`,
                    changeOrigin: false,
                    rewrite: path => path.replace(/^\/api/, ''),
                    configure: setProxyCustomHeaders
                },
                '/login': {
                    target: `http://localhost:${backendPort}/`,
                    changeOrigin: false,
                    configure: setProxyCustomHeaders
                },
                '/login/oauth2': {
                    target: `http://localhost:${backendPort}/`,
                    changeOrigin: false,
                    configure: setProxyCustomHeaders
                },
                '/oauth2': {
                    target: `http://localhost:${backendPort}/`,
                    changeOrigin: false,
                    configure: setProxyCustomHeaders
                },
            },
        },
    }
});
