"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AuthService } from "@/lib/auth";
import type { User } from "@/types/auth";

export default function HomePage() {
  const [user, setUser] = useState<User | null>(null);
  const router = useRouter();

  useEffect(() => {
    const currentUser = AuthService.getCurrentUser();
    console.log("Current user:", currentUser);

    if (!currentUser) {
      // Try to refresh token if not authenticated
      AuthService.refreshToken().then((tokens) => {
        if (tokens) {
          const refreshedUser = AuthService.getCurrentUser();
          console.log("Refreshed user:", refreshedUser);
          setUser(refreshedUser);
        } else {
          router.push("/");
        }
      });
    } else {
      setUser(currentUser);
    }
  }, [router]);

  const handleLogout = () => {
    AuthService.logout();
    router.push("/");
  };

  if (!user) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
        <div className="text-gray-900 dark:text-white">Loading...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <nav className="bg-white dark:bg-gray-800 shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center">
              <h1 className="text-xl font-bold text-gray-900 dark:text-white">
                MachinaEar
              </h1>
            </div>
            <div className="flex items-center">
              <button
                onClick={handleLogout}
                className="ml-4 px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
              >
                Logout
              </button>
            </div>
          </div>
        </div>
      </nav>

      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <div className="px-4 py-6 sm:px-0">
          <div className="bg-white dark:bg-gray-800 shadow rounded-lg p-6">
            <h2 className="text-3xl font-bold text-gray-900 dark:text-white mb-4">
              Hi {user.username || user.email}
            </h2>
            <div className="text-gray-600 dark:text-gray-400">
              <p className="mb-2">Welcome Back!</p>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
