"use client";

import { useEffect, useState, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { exchangeCodeForTokens } from '@/lib/oauth';

function OAuthCallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [error, setError] = useState<string | null>(null);
  const [processing, setProcessing] = useState(true);

  useEffect(() => {
    const handleCallback = async () => {
      try {
        // Get authorization code and state from URL
        const code = searchParams.get('code');
        const state = searchParams.get('state');
        const errorParam = searchParams.get('error');
        const errorDescription = searchParams.get('error_description');

        // Check for errors from authorization server
        if (errorParam) {
          throw new Error(errorDescription || errorParam);
        }

        // Validate required parameters
        if (!code || !state) {
          throw new Error('Missing authorization code or state parameter');
        }

        // Exchange authorization code for tokens
        // Tokens will be set as httpOnly cookies by the backend
        await exchangeCodeForTokens(code, state);

        // Small delay to ensure cookies are fully processed by the browser
        await new Promise(resolve => setTimeout(resolve, 100));

        // Force a full page reload to ensure cookies are available
        // This is more reliable than client-side routing for cookie-based auth
        window.location.href = '/home';
      } catch (err) {
        console.error('OAuth callback error:', err);
        setError(err instanceof Error ? err.message : 'Authentication failed');
        setProcessing(false);
      }
    };

    handleCallback();
  }, [searchParams, router]);

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
        <div className="max-w-md w-full space-y-8 p-8">
          <div className="text-center">
            <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-4">
              Authentication Failed
            </h2>
            <div className="rounded-md bg-red-50 dark:bg-red-900/20 p-4 mb-6">
              <p className="text-sm text-red-800 dark:text-red-400">{error}</p>
            </div>
            <button
              onClick={() => router.push('/')}
              className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
            >
              Back to Login
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
      <div className="text-center">
        <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 dark:border-indigo-400 mb-4"></div>
        <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
          {processing ? 'Completing authentication...' : 'Redirecting...'}
        </h2>
      </div>
    </div>
  );
}

export default function OAuthCallbackPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 dark:border-indigo-400 mb-4"></div>
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
            Loading...
          </h2>
        </div>
      </div>
    }>
      <OAuthCallbackContent />
    </Suspense>
  );
}
