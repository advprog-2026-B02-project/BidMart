import type {NextConfig} from "next";

const nextConfig: NextConfig = {
    experimental: {
        turbopack: {
            // Force Next/Turbopack to treat this directory as the project root.
            root: __dirname,
        },
    },
};

export default nextConfig;
