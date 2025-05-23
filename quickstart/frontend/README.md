# Canton Network Quickstart Frontend

This frontend enables interaction with the backend components of the Canton Network Quickstart.

## Development Server Setup

1. In the parent directory, run.

    ```sh
    make start-vite-dev
    ```
   
    This is the equivalent of `make start`, but with a vite live reload server.
    
    2. In this directory, start the frontend development server, which will reverse proxy requests to the backend.
    
    ```sh
    npm run dev
    ```

    The development server will proxy requests to the backend, which is configured through docker-compose.

## Built with React, TypeScript, and Vite

This project uses Vite with React and TypeScript. To learn more about Vite with React, visit the [Vite](https://vitejs.dev/guide/).
