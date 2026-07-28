import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// The backend serves /api/v1 on 8080. Proxying in dev keeps the browser
// same-origin, so the CORS allowance in the dev profile is a fallback rather
// than something the frontend depends on.
// Override with VITE_API_TARGET in .env.local to point at another backend.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '');
  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: env.VITE_API_TARGET || 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  };
});
