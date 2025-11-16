"use client";

export default function OfflinePage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
      <div className="max-w-md w-full text-center space-y-8 p-8">
        <div>
          <h1 className="text-4xl font-bold text-gray-900 dark:text-white mb-4">
            You're Offline
          </h1>
          <p className="text-lg text-gray-600 dark:text-gray-400 mb-8">
            It looks like you've lost your internet connection. Please check your network and try again.
          </p>
          <div className="space-y-4">
            <button
              onClick={() => window.location.reload()}
              className="w-full px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
            >
              Try Again
            </button>
            <button
              onClick={() => window.history.back()}
              className="w-full px-4 py-2 border border-gray-300 dark:border-gray-700 text-sm font-medium rounded-md text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
            >
              Go Back
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
