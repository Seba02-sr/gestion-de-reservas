// vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { resolve } from 'path';

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    rollupOptions: {
      input: {
        main: resolve(process.cwd(), 'src/scss/main.scss'),
      },
      output: {
        entryFileNames: '[name].js', // opcional, si tienes JS
        assetFileNames: (assetInfo) => {
          if (assetInfo.name?.endsWith('.css')) return 'main.css';
          return 'assets/[name].[ext]';
        },
      },
    },
    cssCodeSplit: true,
  },
});