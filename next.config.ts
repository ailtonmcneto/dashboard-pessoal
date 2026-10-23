import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Cache Components fica desligado: todas as páginas leem a sessão do
  // usuário a cada request, então o modelo dinâmico clássico é mais simples.
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
};

export default nextConfig;
