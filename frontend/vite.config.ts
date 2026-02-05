import path from 'path';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  base: './',
  plugins: [react()],
  define: {
    global: 'globalThis'
  },
  resolve: {
    alias: {
      '@shared': path.resolve(__dirname, '../shared'),
      events: path.resolve(__dirname, 'node_modules/events/events.js')
    }
  },
  optimizeDeps: {
    include: ['buffer', 'events', 'h264-converter']
  },
  server: {
    host: true,
    port: 5173,
    fs: {
      allow: [path.resolve(__dirname, '..')]
    }
  }
});
