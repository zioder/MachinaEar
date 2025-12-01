import type { NextConfig } from 'next';
import withPWA from '@ducanh2912/next-pwa';

const nextConfig: NextConfig = {
  // Disable experimental features that may cause file locking issues on OneDrive
  experimental: {
    webpackBuildWorker: false,
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
              script-src 'self' 'unsafe-eval' 'unsafe-inline';
              style-src 'self' 'unsafe-inline';
              img-src 'self' data: blob:;
              font-src 'self' data:;
              connect-src 'self' https://localhost:8443 wss://localhost:3000;
              frame-ancestors 'none';
              base-uri 'self';
              form-action 'self';
            `.replace(/\s{2,}/g, ' ').trim(),
          },
        ],
      },
    ];
  },
};

export default withPWA({
  dest: 'public',
  disable: false, // Enable PWA in both development and production
  register: true,
})(nextConfig);
