"use client";

import { useEffect, useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { API_ENDPOINTS } from "@/lib/constants";
import { LoadingSpinner } from "@/components/ui";

export default function VerifyEmailPage() {
    const searchParams = useSearchParams();
    const router = useRouter();
    const [status, setStatus] = useState<"verifying" | "success" | "error">("verifying");
    const [error, setError] = useState<string>("");

    useEffect(() => {
        const token = searchParams.get("token");
        if (!token) {
            setStatus("error");
            setError("Verification token is missing.");
            return;
        }

        const verify = async () => {
            try {
                const response = await fetch(`${API_ENDPOINTS.VERIFY_EMAIL}?token=${encodeURIComponent(token)}`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                });

                const result = await response.json();

                if (response.ok) {
                    setStatus("success");
                    // Redirect to login after 3 seconds
                    setTimeout(() => {
                        router.push("/");
                    }, 3000);
                } else {
                    setStatus("error");
                    setError(result.error || "Verification failed. The link may be expired.");
                }
            } catch (err) {
                setStatus("error");
                setError("An unexpected error occurred. Please try again later.");
            }
        };

        verify();
    }, [searchParams, router]);

    return (
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 p-4">
            <div className="max-w-md w-full bg-white/10 backdrop-blur-xl border border-white/20 rounded-2xl p-8 shadow-2xl text-center">
                {status === "verifying" && (
                    <>
                        <div className="w-16 h-16 bg-indigo-500/20 rounded-full flex items-center justify-center mx-auto mb-6">
                            <LoadingSpinner />
                        </div>
                        <h1 className="text-2xl font-bold text-white mb-2">Verifying Email</h1>
                        <p className="text-gray-400">Please wait while we confirm your email address...</p>
                    </>
                )}

                {status === "success" && (
                    <>
                        <div className="w-16 h-16 bg-green-500/20 rounded-full flex items-center justify-center mx-auto mb-6">
                            <span className="text-3xl text-green-500">✓</span>
                        </div>
                        <h1 className="text-2xl font-bold text-white mb-2">Email Verified!</h1>
                        <p className="text-gray-300 mb-6">Your email has been successfully verified. You're all set!</p>
                        <p className="text-sm text-gray-400">Redirecting you to sign in...</p>
                    </>
                )}

                {status === "error" && (
                    <>
                        <div className="w-16 h-16 bg-red-500/20 rounded-full flex items-center justify-center mx-auto mb-6">
                            <span className="text-3xl text-red-500">✕</span>
                        </div>
                        <h1 className="text-2xl font-bold text-white mb-2">Verification Failed</h1>
                        <p className="text-red-400 mb-6">{error}</p>
                        <button
                            onClick={() => router.push("/")}
                            className="w-full py-3 px-6 bg-white/10 hover:bg-white/20 text-white font-semibold rounded-xl transition-all border border-white/10"
                        >
                            Back to Sign In
                        </button>
                    </>
                )}
            </div>
        </div>
    );
}
