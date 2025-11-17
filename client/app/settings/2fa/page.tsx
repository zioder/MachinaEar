"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

export default function TwoFactorSettingsPage() {
  const router = useRouter();

  useEffect(() => {
    // Redirect to the new settings page
    router.replace("/settings");
  }, [router]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
      <div className="text-gray-900 dark:text-white">Redirecting to settings...</div>
    </div>
  );
}
