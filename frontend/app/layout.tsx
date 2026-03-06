import type { Metadata } from "next";
import React from "react";
import { Plus_Jakarta_Sans } from "next/font/google";
import "./globals.css";

const plusJakarta = Plus_Jakarta_Sans({
    subsets: ["latin"],
    weight: ["300", "400", "500", "600", "700"],
    variable: "--font-plus-jakarta",
});

export const metadata: Metadata = {
    title: "BidMart",
    description: "BidMart Auth",
};

export default function RootLayout({
                                       children,
                                   }: Readonly<{ children: React.ReactNode }>) {
    return (
        <html lang="id" className={plusJakarta.variable}>
        <body className="font-sans">{children}</body>
        </html>
    );
}