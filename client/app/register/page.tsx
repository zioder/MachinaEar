"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { Button, Input, Alert } from "@/components/ui";
import { useAuth } from "@/hooks";
import { validatePassword, validatePasswordMatch, getPasswordStrength } from "@/lib/validation";

export default function RegisterPage() {
  const [email, setEmail] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState("");
  const router = useRouter();
  
  const { register, isAuthenticated, loading } = useAuth();

  useEffect(() => {
    if (isAuthenticated) {
      router.push("/home");
    }
  }, [isAuthenticated, router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    // Validate password
    const passwordValidation = validatePassword(password);
    if (!passwordValidation.valid) {
      setError(passwordValidation.error!);
      return;
    }

    // Validate passwords match
    const matchValidation = validatePasswordMatch(password, confirmPassword);
    if (!matchValidation.valid) {
      setError(matchValidation.error!);
      return;
    }

    const result = await register({ email, username, password, confirmPassword });
    if (!result.success && result.error) {
      setError(result.error);
    }
  };

  const passwordStrength = password ? getPasswordStrength(password) : null;

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900 dark:text-white">
            Create your account
          </h2>
        </div>
        
        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          <div className="space-y-4">
            <Input
              id="email-address"
              name="email"
              type="email"
              autoComplete="email"
              required
              placeholder="Email address"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            
            <Input
              id="username"
              name="username"
              type="text"
              autoComplete="username"
              required
              placeholder="Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
            
            <Input
              id="password"
              label="Password"
              name="password"
              type="password"
              autoComplete="new-password webauthn"
              required
              placeholder="Enter a strong password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              helperText="At least 12 characters with 3 of: uppercase, lowercase, numbers, symbols"
            />
            
            {passwordStrength && (
              <div className="text-sm">
                <div className="flex items-center gap-2">
                  <div className="flex-1 h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
                    <div
                      className={`h-full transition-all ${
                        passwordStrength.level === 'weak' ? 'bg-red-500 w-1/4' :
                        passwordStrength.level === 'medium' ? 'bg-yellow-500 w-2/4' :
                        passwordStrength.level === 'strong' ? 'bg-green-500 w-3/4' :
                        'bg-green-600 w-full'
                      }`}
                    />
                  </div>
                  <span className="text-gray-600 dark:text-gray-400 min-w-fit">
                    {passwordStrength.feedback}
                  </span>
                </div>
              </div>
            )}
            
            <Input
              id="confirm-password"
              label="Confirm Password"
              name="confirm-password"
              type="password"
              autoComplete="new-password"
              required
              placeholder="Confirm password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
            />
          </div>

          {error && <Alert variant="error">{error}</Alert>}

          <div>
            <Button type="submit" fullWidth loading={loading}>
              Register
            </Button>
          </div>
        </form>

        <div className="text-center">
          <p className="text-sm text-gray-600 dark:text-gray-400">
            Already have an account?{" "}
            <button
              onClick={() => router.push("/")}
              className="font-medium text-indigo-600 hover:text-indigo-500 dark:text-indigo-400 dark:hover:text-indigo-300"
            >
              Sign in here
            </button>
          </p>
        </div>
      </div>
    </div>
  );
}
