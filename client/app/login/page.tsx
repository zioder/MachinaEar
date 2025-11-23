"use client";

import { useState, useEffect, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Button, Input, Alert, LoadingSpinner } from "@/components/ui";
import { useAuth } from "@/hooks";

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { login, isAuthenticated } = useAuth();
  
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [totpCode, setTotpCode] = useState("");
  const [recoveryCode, setRecoveryCode] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [requires2FA, setRequires2FA] = useState(false);

  const returnTo = searchParams.get("returnTo");

  useEffect(() => {
    if (isAuthenticated) {
      if (returnTo) {
        window.location.href = `http://localhost:8080/iam-0.1.0/iam${returnTo}`;
      } else {
        router.push("/home");
      }
    }
  }, [isAuthenticated, returnTo, router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const result = await login({
        email,
        password,
        totpCode: totpCode ? parseInt(totpCode) : undefined,
        recoveryCode: recoveryCode || undefined,
      }, returnTo || undefined);

      if (result.requires2FA) {
        setRequires2FA(true);
        setError("Two-factor authentication required");
      } else if (!result.success && result.error) {
        setError(result.error);
      }
    } catch (err: any) {
      setError(err.message || "Login failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-indigo-500 via-purple-500 to-pink-500 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8 bg-white dark:bg-gray-800 rounded-2xl shadow-2xl p-10">
        <div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900 dark:text-white">
            Sign in to MachinaEar
          </h2>
          <p className="mt-2 text-center text-sm text-gray-600 dark:text-gray-400">
            Enter your credentials to continue
          </p>
        </div>

        {error && <Alert variant="error">{error}</Alert>}

        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          <div className="space-y-4">
            <Input
              id="email"
              name="email"
              type="email"
              autoComplete="email"
              required
              disabled={requires2FA}
              placeholder="Email address"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            
            <Input
              id="password"
              name="password"
              type="password"
              autoComplete="current-password"
              required
              disabled={requires2FA}
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          {requires2FA && (
            <div className="space-y-2">
              <Input
                id="totpCode"
                name="totpCode"
                type="text"
                maxLength={6}
                placeholder="6-digit authenticator code"
                value={totpCode}
                onChange={(e) => setTotpCode(e.target.value)}
              />
              <div className="text-center text-sm text-gray-600 dark:text-gray-400">
                or
              </div>
              <Input
                id="recoveryCode"
                name="recoveryCode"
                type="text"
                placeholder="Recovery code"
                value={recoveryCode}
                onChange={(e) => setRecoveryCode(e.target.value)}
                className="font-mono"
              />
            </div>
          )}

          <div>
            <Button type="submit" fullWidth loading={loading}>
              Sign in
            </Button>
          </div>

          <div className="text-center">
            <p className="text-sm text-gray-600 dark:text-gray-400">
              Don't have an account?{" "}
              <button
                type="button"
                onClick={() => router.push("/register")}
                className="font-medium text-indigo-600 hover:text-indigo-500 dark:text-indigo-400 dark:hover:text-indigo-300"
              >
                Register here
              </button>
            </p>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<LoadingSpinner fullScreen message="Loading..." />}>
      <LoginForm />
    </Suspense>
  );
}
