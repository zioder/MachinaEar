"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks";
import { initiateOAuthFlow } from "@/lib/oauth";
import { LoadingSpinner } from "@/components/ui";

/**
 * Root page - shows landing page with sign-in/sign-up options, or redirects to /home if authenticated
 * Authentication is handled by the IAM server
 */
export default function RootPage() {
  const router = useRouter();
  // Temporarily disable auth check to show the page
  const isAuthenticated = false;
  const loading = false;

  // useEffect(() => {
  //   if (!loading && isAuthenticated) {
  //     router.push("/home");
  //   }
  // }, [isAuthenticated, loading, router]);

  // if (loading) {
  //   return <LoadingSpinner fullScreen message="Loading..." />;
  // }

  // if (isAuthenticated) {
  //   return <LoadingSpinner fullScreen message="Redirecting..." />;
  // }

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

          {/* Auth button */}
          <div>
            <button
              type="button"
              onClick={() => router.push('/home')}
              className="w-full py-4 px-6 bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white font-semibold rounded-xl shadow-lg shadow-indigo-500/30 transition-all duration-200 transform hover:scale-[1.02] active:scale-[0.98]"
            >
              Get Started
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
