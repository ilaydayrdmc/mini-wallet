import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Tarayici /api/... istegini Vite'a atar, Vite bunu backend'e iletir.
      // Boylece ayni origin gibi calisir ve CORS ayari gerekmez.
      '/api': 'http://localhost:8080',
    },
  },
})
