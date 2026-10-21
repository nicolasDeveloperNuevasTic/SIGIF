/**
 * @file vite.config.js
 * @description Configuración de Vite: React, PWA (Service Worker para operación offline) y proxy al API.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';

export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      manifest: {
        name: 'SIGIF - Ferretería',
        short_name: 'SIGIF',
        description: 'Inventario, compras y ventas de ferretería',
        theme_color: '#1b4f72',
        background_color: '#ffffff',
        display: 'standalone',
        icons: [{ src: 'icono.svg', sizes: 'any', type: 'image/svg+xml' }],
      },
      workbox: {
        // El shell de la aplicación queda en caché; los datos offline se manejan con IndexedDB.
        globPatterns: ['**/*.{js,css,html,svg,woff2}'],
        navigateFallback: '/index.html',
        navigateFallbackDenylist: [/^\/api\//],
      },
    }),
  ],
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          react: ['react', 'react-dom', 'react-router-dom'],
          mui: ['@mui/material', '@mui/icons-material', '@emotion/react', '@emotion/styled'],
          formularios: ['formik', 'yup', 'i18next', 'react-i18next', 'axios', 'idb'],
        },
      },
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': { target: process.env.VITE_API_PROXY || 'http://localhost:8080', changeOrigin: true },
    },
  },
});
