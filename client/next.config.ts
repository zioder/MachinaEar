import type { NextConfig } from 'next';
import withPWA from '@ducanh2912/next-pwa';

const nextConfig: NextConfig = {
  output: 'standalone',
  // Disable experimental features that may cause file locking issues on OneDrive
  experimental: {
    webpackBuildWorker: false,
  },

  // Proxy API requests to backend to avoid cross-origin cookie issues
  // This allows cookies set by the backend to be sent on subsequent requests
  async rewrites() {
    return [
      {
        source: '/api/iam/:path*',
        destination: 'http://localhost:8080/iam-0.1.0/iam/:path*',
      },
    ];
  },

  // Security headers
  async headers() {
    return [
      {
        source: '/:path*',
        headers: [
          {
            key: 'X-Frame-Options',
            value: 'DENY',
          },
          {
            key: 'X-Content-Type-Options',
            value: 'nosniff',
          },
          {
            key: 'Referrer-Policy',
            value: 'strict-origin-when-cross-origin',
          },
          {
            key: 'X-XSS-Protection',
            value: '1; mode=block',
          },
          {
            key: 'Permissions-Policy',
            value: 'camera=(), microphone=(), geolocation=()',
          },
          // Content Security Policy
          // Note: 'unsafe-inline' and 'unsafe-eval' needed for Next.js dev mode
          // In production, consider removing these for stricter security
          {
            key: 'Content-Security-Policy',
            value: `
              default-src 'self';
              script-src 'self' 'unsafe-eval' 'unsafe-inline' https://vercel.live;
              style-src 'self' 'unsafe-inline';
              img-src 'self' data: blob: https://vercel.com;
              font-src 'self' data:;
              connect-src 'self' https://iam.machinaear.me http://localhost:8081 https://localhost:8081 http://localhost:8080 https://localhost:8080 wss://localhost:3000 https://vercel.live https://vitals.vercel-insights.com;
              frame-ancestors 'none';
              base-uri 'self';
              form-action 'self';
            `.replace(/\s{2,}/g, ' ').trim(),
          },
        ],
      },
    ];
  },
  eslint: {
    ignoreDuringBuilds: true,
  },
  typescript: {
    ignoreBuildErrors: true,
  },
};

export default withPWA({
  dest: 'public',
  disable: true, // Disable PWA to avoid service worker errors
  register: false,
})(nextConfig);
