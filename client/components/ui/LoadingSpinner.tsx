import React from 'react';

interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg';
  message?: string;
  fullScreen?: boolean;
}

export function LoadingSpinner({ size = 'md', message, fullScreen = false }: LoadingSpinnerProps) {
  const sizeStyles = {
    sm: 'h-6 w-6',
    md: 'h-12 w-12',
    lg: 'h-16 w-16',
  };
  
  const spinner = (
    <div className="text-center">
      <div className={`inline-block animate-spin rounded-full border-b-2 border-indigo-600 dark:border-indigo-400 ${sizeStyles[size]} mb-4`}></div>
      {message && (
        <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
          {message}
        </h2>
      )}
    </div>
  );
  
  if (fullScreen) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
        {spinner}
      </div>
    );
  }
  
  return spinner;
}
