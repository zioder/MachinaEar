"use client";

import { Suspense, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useAuth } from "@/hooks";
import { initiateOAuthFlow, initiateGoogleOAuthFlow } from "@/lib/oauth";
import { LoadingSpinner } from "@/components/ui";

/**
 * Inner component that uses useSearchParams (must be wrapped in Suspense)
 */
function RootPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { isAuthenticated, loading } = useAuth();

  useEffect(() => {
    if (!loading && isAuthenticated) {
      router.push("/home");
    }
  }, [isAuthenticated, loading, router]);

  // Check for auto_login parameter (user logged in via IAM directly, e.g., after password reset)
  useEffect(() => {
    if (!loading && !isAuthenticated) {
      const autoLogin = searchParams.get('auto_login');
      if (autoLogin === 'true') {
        // Clear the URL parameter and initiate OAuth flow
        // The user already has a session on IAM, so this will complete automatically
        window.history.replaceState({}, '', '/');
        initiateOAuthFlow();
      }
    }
  }, [loading, isAuthenticated, searchParams]);

  if (loading) {
    return <LoadingSpinner fullScreen message="Loading..." />;
  }

  // Show loading if auto_login is pending
  const autoLogin = searchParams.get('auto_login');
  if (autoLogin === 'true') {
    return <LoadingSpinner fullScreen message="Signing in..." />;
  }

  if (isAuthenticated) {
    return <LoadingSpinner fullScreen message="Redirecting..." />;
  }

  return (
    <div className="min-h-screen flex flex-col bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900">
      {/* Animated background elements */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-40 -right-40 w-80 h-80 bg-purple-500 rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-pulse" />
        <div className="absolute -bottom-40 -left-40 w-80 h-80 bg-indigo-500 rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-pulse" style={{ animationDelay: "2s" }} />
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-pink-500 rounded-full mix-blend-multiply filter blur-3xl opacity-10 animate-pulse" style={{ animationDelay: "4s" }} />
      </div>

      {/* Main content */}
      <div className="flex-1 flex items-center justify-center px-4 sm:px-6 lg:px-8 relative z-10">
        <div className="max-w-lg w-full space-y-10">
          {/* Logo and branding */}
          <div className="text-center">
            <div className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-2xl shadow-2xl mb-6">
              <svg
                className="w-12 h-12 text-white"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={1.5}
                  d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"
                />
              </svg>
            </div>
            <h1 className="text-5xl font-bold bg-gradient-to-r from-white via-purple-200 to-indigo-200 bg-clip-text text-transparent">
              MachinaEar
            </h1>
            <p className="mt-4 text-xl text-gray-300">
              Industrial IoT Monitoring Platform
            </p>
            <p className="mt-2 text-gray-400 max-w-md mx-auto">
              Monitor your machines in real-time with AI-powered predictive maintenance and anomaly detection
            </p>
          </div>

          {/* Feature highlights */}
          <div className="grid grid-cols-3 gap-4 text-center">
            <div className="p-4 rounded-xl bg-white/5 backdrop-blur-sm border border-white/10">
              <div className="text-2xl mb-2">📊</div>
              <p className="text-sm text-gray-300">Real-time Analytics</p>
            </div>
            <div className="p-4 rounded-xl bg-white/5 backdrop-blur-sm border border-white/10">
              <div className="text-2xl mb-2">🤖</div>
              <p className="text-sm text-gray-300">AI Detection</p>
            </div>
            <div className="p-4 rounded-xl bg-white/5 backdrop-blur-sm border border-white/10">
              <div className="text-2xl mb-2">🔔</div>
              <p className="text-sm text-gray-300">Smart Alerts</p>
            </div>
          </div>

          {/* Auth buttons */}
          <div className="space-y-3">
            <button
              onClick={() => initiateOAuthFlow()}
              className="w-full py-4 px-6 bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white font-semibold rounded-xl shadow-lg shadow-indigo-500/30 transition-all duration-200 transform hover:scale-[1.02] active:scale-[0.98]"
            >
              Get Started
            </button>

            {/* Divider */}
            <div className="relative">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-gray-600"></div>
              </div>
              <div className="relative flex justify-center text-sm">
                <span className="px-2 bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 text-gray-400">
                  or
                </span>
              </div>
            </div>

            {/* Google Sign In */}
            <button
              onClick={() => initiateGoogleOAuthFlow()}
              className="w-full py-4 px-6 bg-white hover:bg-gray-50 text-gray-700 font-semibold rounded-xl shadow-lg border border-gray-300 transition-all duration-200 transform hover:scale-[1.02] active:scale-[0.98] flex items-center justify-center gap-3"
            >
              <svg className="w-5 h-5" viewBox="0 0 24 24">
                <path
                  fill="#4285F4"
                  d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                />
                <path
                  fill="#34A853"
                  d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                />
                <path
                  fill="#FBBC05"
                  d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                />
                <path
                  fill="#EA4335"
                  d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                />
              </svg>
              Sign in with Google
            </button>

            
          </div>

        </div>
      </div>

      {/* Footer */}
      <footer className="relative z-10 py-6 text-center text-gray-500 text-sm">
        <p>© 2026 MachinaEar. Built for industrial excellence.</p>
      </footer>
    </div>
  );
}

/**
 * Root page - shows landing page with sign-in/sign-up options, or redirects to /home if authenticated
 * Authentication is handled by the IAM server
 */
export default function RootPage() {
  return (
    <Suspense fallback={<LoadingSpinner fullScreen message="Loading..." />}>
      <RootPageContent />
    </Suspense>
  );
}
