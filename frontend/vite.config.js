import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json-summary'],
      include: ['src/App.jsx', 'src/services/api.js'],
      thresholds: {
        lines: 75,
        statements: 75,
        functions: 75,
        branches: 70,
      },
    },
  },
})
