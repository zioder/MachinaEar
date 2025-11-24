"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { AuthService } from "@/lib/auth";
import { use2FA } from "@/hooks";
import { Button, Alert, LoadingSpinner } from "@/components/ui";
import {
  QRCodeSetup,
  VerificationForm,
  RecoveryCodesDisplay,
  TwoFactorManagement,
} from "@/components/2fa";
import type { User } from "@/types/auth";

type Tab = "security";

export default function SettingsPage() {
  const [user, setUser] = useState<User | null>(null);
  const [activeTab, setActiveTab] = useState<Tab>("security");
  const [success, setSuccess] = useState("");
  const [showRecoveryCodes, setShowRecoveryCodes] = useState(false);
  const router = useRouter();
  
  const { setup, loading, error, initiate2FASetup, enable2FA, disable2FA, regenerateRecoveryCodes } = use2FA();

  useEffect(() => {
    const loadUser = async () => {
      const currentUser = await AuthService.getCurrentUser();
      if (!currentUser) {
        router.push("/");
        return;
      }
      setUser(currentUser);
    };
    loadUser();
  }, [router]);

  const handleSetup2FA = async () => {
    if (!user) return;
    setSuccess("");
    
    const result = await initiate2FASetup(user.email);
    if (result.success) {
      setSuccess("Scan the QR code with your authenticator app");
    }
  };

  const handleEnable2FA = async (code: string) => {
    if (!user || !setup) return;
    
    const codeNum = parseInt(code);
    if (isNaN(codeNum)) {
      return;
    }

    const result = await enable2FA(user.email, setup.secret, codeNum, setup.recoveryCodes);
    if (result.success) {
      setSuccess("Two-factor authentication enabled successfully!");
      setShowRecoveryCodes(true);
    }
  };

  const handleDisable2FA = async (password: string) => {
    if (!user) return;
    
    const result = await disable2FA(user.email, password);
    if (result.success) {
      setSuccess("Two-factor authentication disabled");
      setShowRecoveryCodes(false);
    }
  };

  const handleRegenerateCodes = async (password: string) => {
    if (!user) return;

    if (!confirm("This will invalidate your old recovery codes. Continue?")) {
      return;
    }
    
    const result = await regenerateRecoveryCodes(user.email, password);
    if (result.success) {
      setSuccess("Recovery codes regenerated successfully!");
      setShowRecoveryCodes(true);
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
    return <LoadingSpinner fullScreen />;
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
                    {error && <Alert variant="error">{error}</Alert>}
                    {success && <Alert variant="success">{success}</Alert>}

                    {!setup ? (
                      <div>
                        <p className="text-gray-700 dark:text-gray-300 mb-4">
                          Add an extra layer of security to your account by enabling two-factor authentication (2FA).
                          You'll need to enter a code from your authenticator app when you sign in.
                        </p>
                        <Button onClick={handleSetup2FA} loading={loading}>
                          Enable Two-Factor Authentication
                        </Button>
                      </div>
                    ) : (
                      <div className="space-y-6">
                        <QRCodeSetup qrCodeImage={setup.qrCodeImage} secret={setup.secret} />

                        {!showRecoveryCodes && (
                          <VerificationForm onVerify={handleEnable2FA} loading={loading} />
                        )}

                        {showRecoveryCodes && (
                          <RecoveryCodesDisplay
                            recoveryCodes={setup.recoveryCodes}
                            onDownload={downloadRecoveryCodes}
                          />
                        )}

                        <TwoFactorManagement
                          onRegenerateCodes={handleRegenerateCodes}
                          onDisable2FA={handleDisable2FA}
                          loading={loading}
                        />
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
