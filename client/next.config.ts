import type { NextConfig } from 'next';
import withPWA from 'next-pwa';

const nextConfig: NextConfig = {
  // Disable experimental features that may cause file locking issues on OneDrive
  experimental: {
    webpackBuildWorker: false,
  },
};

export default withPWA({
  dest: 'public',
  disable: false, // Enable PWA in both development and production
  register: true,
  skipWaiting: true,
})(nextConfig);
