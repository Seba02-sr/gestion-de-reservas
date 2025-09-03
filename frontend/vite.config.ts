// vite.config.ts
import { defineConfig } from "vite"
import react from "@vitejs/plugin-react"
import { fileURLToPath, URL } from "node:url"

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  build: {
    outDir: "dist",
    emptyOutDir: true,
    rollupOptions: {
      input: {
        // si también tenés una entrada HTML/TSX principal, dejala;
        // esta línea es para tu CSS principal
        main: "src/scss/main.scss",
      },
      output: {
        entryFileNames: "[name].js",
        assetFileNames: (assetInfo) => {
          if (assetInfo.name?.endsWith(".css")) return "main.css"
          return "assets/[name].[ext]"
        },
      },
    },
    cssCodeSplit: true,
  },
})
