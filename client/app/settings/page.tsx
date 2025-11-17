"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { AuthService } from "@/lib/auth";
import type { User, TwoFactorSetup } from "@/types/auth";

type Tab = "security";

export default function SettingsPage() {
  const [user, setUser] = useState<User | null>(null);
  const [activeTab, setActiveTab] = useState<Tab>("security");
  const router = useRouter();

  // 2FA States
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [password, setPassword] = useState("");
  const [verificationCode, setVerificationCode] = useState("");
  const [setup, setSetup] = useState<TwoFactorSetup | null>(null);
  const [showRecoveryCodes, setShowRecoveryCodes] = useState(false);

  useEffect(() => {
    const currentUser = AuthService.getCurrentUser();
    if (!currentUser) {
      router.push("/");
      return;
    }
    setUser(currentUser);
  }, [router]);

  const handleSetup2FA = async () => {
    if (!user) return;

    setLoading(true);
    setError("");
    setSuccess("");

    try {
      const setupData = await AuthService.setup2FA(user.email);
      setSetup(setupData);
      setSuccess("Scan the QR code with your authenticator app");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to setup 2FA");
    } finally {
      setLoading(false);
    }
  };

  const handleEnable2FA = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !setup) return;

    setLoading(true);
    setError("");

    try {
      const code = parseInt(verificationCode);
      if (isNaN(code)) {
        setError("Invalid verification code");
        return;
      }

      await AuthService.enable2FA(user.email, setup.secret, code, setup.recoveryCodes);
      setSuccess("Two-factor authentication enabled successfully!");
      setShowRecoveryCodes(true);
      setVerificationCode("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to enable 2FA");
    } finally {
      setLoading(false);
    }
  };

  const handleDisable2FA = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;

    if (!confirm("Are you sure you want to disable two-factor authentication?")) {
      return;
    }

    setLoading(true);
    setError("");

    try {
      await AuthService.disable2FA(user.email, password);
      setSuccess("Two-factor authentication disabled");
      setPassword("");
      setSetup(null);
      setShowRecoveryCodes(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to disable 2FA");
    } finally {
      setLoading(false);
    }
  };

  const handleRegenerateCodes = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;

    if (!confirm("This will invalidate your old recovery codes. Continue?")) {
      return;
    }

    setLoading(true);
    setError("");

    try {
      const newCodes = await AuthService.regenerateRecoveryCodes(user.email, password);
      if (setup) {
        setSetup({ ...setup, recoveryCodes: newCodes });
      }
      setSuccess("Recovery codes regenerated successfully!");
      setShowRecoveryCodes(true);
      setPassword("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to regenerate recovery codes");
    } finally {
      setLoading(false);
    }
  };

  const downloadRecoveryCodes = () => {
    if (!setup) return;

    const codesText = setup.recoveryCodes.join("\n");
    const blob = new Blob([`MachinaEar Recovery Codes\n\n${codesText}\n\nKeep these codes in a safe place!`], {
      type: "text/plain",
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "machinaear-recovery-codes.txt";
    a.click();
    URL.revokeObjectURL(url);
  };

  if (!user) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <nav className="bg-white dark:bg-gray-800 shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center">
              <button
                onClick={() => router.push("/home")}
                className="text-indigo-600 hover:text-indigo-500 dark:text-indigo-400 dark:hover:text-indigo-300 flex items-center"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  strokeWidth={1.5}
                  stroke="currentColor"
                  className="w-5 h-5 mr-2"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18"
                  />
                </svg>
                Back to Home
              </button>
            </div>
          </div>
        </div>
      </nav>

      <div className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <div className="px-4 sm:px-0">
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-6">
            Settings
          </h1>

          <div className="bg-white dark:bg-gray-800 shadow rounded-lg overflow-hidden">
            {/* Tabs */}
            <div className="border-b border-gray-200 dark:border-gray-700">
              <nav className="flex -mb-px">
                <button
                  onClick={() => setActiveTab("security")}
                  className={`px-6 py-4 text-sm font-medium border-b-2 ${
                    activeTab === "security"
                      ? "border-indigo-500 text-indigo-600 dark:text-indigo-400"
                      : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-300"
                  }`}
                >
                  Security
                </button>
              </nav>
            </div>

            {/* Tab Content */}
            <div className="p-6">
              {activeTab === "security" && (
                <div>
                  <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">
                    Two-Factor Authentication
                  </h2>

                  <div className="space-y-6">
                    {error && (
                      <div className="rounded-md bg-red-50 dark:bg-red-900/20 p-4">
                        <p className="text-sm text-red-800 dark:text-red-400">{error}</p>
                      </div>
                    )}

                    {success && (
                      <div className="rounded-md bg-green-50 dark:bg-green-900/20 p-4">
                        <p className="text-sm text-green-800 dark:text-green-400">{success}</p>
                      </div>
                    )}

                    {!setup ? (
                      <div>
                        <p className="text-gray-700 dark:text-gray-300 mb-4">
                          Add an extra layer of security to your account by enabling two-factor authentication (2FA).
                          You'll need to enter a code from your authenticator app when you sign in.
                        </p>
                        <button
                          onClick={handleSetup2FA}
                          disabled={loading}
                          className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          {loading ? "Setting up..." : "Enable Two-Factor Authentication"}
                        </button>
                      </div>
                    ) : (
                      <div className="space-y-6">
                        <div>
                          <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
                            Step 1: Scan QR Code
                          </h3>
                          <p className="text-gray-700 dark:text-gray-300 mb-4">
                            Scan this QR code with your authenticator app (It's highly recommended to use Authy):
                          </p>
                          <div className="bg-white p-4 rounded-lg inline-block">
                            <img
                              src={`data:image/png;base64,${setup.qrCodeImage}`}
                              alt="2FA QR Code"
                              className="w-64 h-64"
                            />
                          </div>
                          <p className="text-sm text-gray-500 dark:text-gray-400 mt-2">
                            Or manually enter this secret: <code className="font-mono">{setup.secret}</code>
                          </p>
                        </div>

                        {!showRecoveryCodes && (
                          <form onSubmit={handleEnable2FA}>
                            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
                              Step 2: Verify Code
                            </h3>
                            <p className="text-gray-700 dark:text-gray-300 mb-4">
                              Enter the 6-digit code from your authenticator app to complete setup:
                            </p>
                            <div className="flex items-center gap-4">
                              <input
                                type="text"
                                value={verificationCode}
                                onChange={(e) => setVerificationCode(e.target.value.replace(/\D/g, ""))}
                                maxLength={6}
                                pattern="[0-9]{6}"
                                required
                                placeholder="000000"
                                className="w-32 px-3 py-2 border border-gray-300 dark:border-gray-700 rounded-md text-center text-lg font-mono focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                              />
                              <button
                                type="submit"
                                disabled={loading || verificationCode.length !== 6}
                                className="px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed"
                              >
                                {loading ? "Verifying..." : "Verify & Enable"}
                              </button>
                            </div>
                          </form>
                        )}

                        {showRecoveryCodes && (
                          <div>
                            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
                              Recovery Codes
                            </h3>
                            <div className="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-md p-4 mb-4">
                              <p className="text-sm text-yellow-800 dark:text-yellow-200 font-semibold mb-2">
                                Save these recovery codes in a safe place!
                              </p>
                              <p className="text-sm text-yellow-700 dark:text-yellow-300">
                                Each code can only be used once. You'll need them if you lose access to your authenticator app.
                              </p>
                            </div>
                            <div className="bg-gray-100 dark:bg-gray-700 rounded-md p-4 mb-4">
                              <div className="grid grid-cols-2 gap-2 font-mono text-sm">
                                {setup.recoveryCodes.map((code, index) => (
                                  <div key={index} className="text-gray-900 dark:text-white">
                                    {index + 1}. {code}
                                  </div>
                                ))}
                              </div>
                            </div>
                            <button
                              onClick={downloadRecoveryCodes}
                              className="inline-flex items-center px-4 py-2 border border-gray-300 dark:border-gray-600 text-sm font-medium rounded-md text-gray-700 dark:text-gray-200 bg-white dark:bg-gray-700 hover:bg-gray-50 dark:hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
                            >
                              Download Recovery Codes
                            </button>
                          </div>
                        )}

                        <div className="border-t border-gray-200 dark:border-gray-700 pt-6 mt-6">
                          <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
                            Manage Two-Factor Authentication
                          </h3>

                          <div className="space-y-4">
                            <form onSubmit={handleRegenerateCodes} className="space-y-4">
                              <div>
                                <label htmlFor="password-regen" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                                  Regenerate Recovery Codes
                                </label>
                                <p className="text-sm text-gray-500 dark:text-gray-400 mb-2">
                                  Generate new recovery codes (this will invalidate old codes)
                                </p>
                                <div className="flex items-center gap-4">
                                  <input
                                    id="password-regen"
                                    type="password"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    placeholder="Enter your password"
                                    required
                                    className="flex-1 max-w-xs px-3 py-2 border border-gray-300 dark:border-gray-700 rounded-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                                  />
                                  <button
                                    type="submit"
                                    disabled={loading}
                                    className="px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed"
                                  >
                                    Regenerate
                                  </button>
                                </div>
                              </div>
                            </form>

                            <form onSubmit={handleDisable2FA} className="space-y-4">
                              <div>
                                <label htmlFor="password-disable" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                                  Disable Two-Factor Authentication
                                </label>
                                <p className="text-sm text-gray-500 dark:text-gray-400 mb-2">
                                  Turn off 2FA for your account
                                </p>
                                <div className="flex items-center gap-4">
                                  <input
                                    id="password-disable"
                                    type="password"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    placeholder="Enter your password"
                                    required
                                    className="flex-1 max-w-xs px-3 py-2 border border-gray-300 dark:border-gray-700 rounded-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                                  />
                                  <button
                                    type="submit"
                                    disabled={loading}
                                    className="px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 disabled:opacity-50 disabled:cursor-not-allowed"
                                  >
                                    Disable 2FA
                                  </button>
                                </div>
                              </div>
                            </form>
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
