import type { Metadata } from "next";
import localFont from "next/font/local";
import "./globals.css";

// Fonte servida pelo próprio app (pacote @fontsource), sem depender do Google Fonts no build.
const bricolage = localFont({
  src: "../../node_modules/@fontsource-variable/bricolage-grotesque/files/bricolage-grotesque-latin-wght-normal.woff2",
  weight: "200 800",
  variable: "--font-bricolage",
  display: "swap",
});

export const metadata: Metadata = {
  title: "Vida",
  description: "Painel pessoal: finanças, hábitos, saúde, tarefas, agenda e estudos.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="pt-BR" className={`${bricolage.variable} h-full antialiased`}>
      <body className="min-h-full">{children}</body>
    </html>
  );
}
