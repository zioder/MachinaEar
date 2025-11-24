"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks";
import { LoadingSpinner } from "@/components/ui";

/**
 * Root page - redirects to /home if authenticated, otherwise to /login
 * Simplified entry point that delegates to dedicated login page
 */
export default function RootPage() {
  const router = useRouter();
  const { isAuthenticated, loading } = useAuth();

  useEffect(() => {
    if (!loading) {
      if (isAuthenticated) {
        router.push("/home");
      } else {
        router.push("/login");
      }
    }
  }, [isAuthenticated, loading, router]);

  return <LoadingSpinner fullScreen message="Loading..." />;
}
