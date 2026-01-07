/**
 * Enhanced API client with better error handling and type safety
 */

import { parseApiError, NetworkError } from './errors';

export interface RequestConfig extends RequestInit {
  timeout?: number;
}

/**
 * Base API client with automatic error handling
 */
export class ApiClient {
  private baseURL: string;
  private defaultTimeout: number = 30000; // 30 seconds

  constructor(baseURL: string) {
    this.baseURL = baseURL;
  }

  /**
   * Make an HTTP request with proper error handling
   */
  async request<T = any>(
    endpoint: string,
    config: RequestConfig = {}
  ): Promise<T> {
    const { timeout = this.defaultTimeout, ...fetchConfig } = config;

    const url = endpoint.startsWith('http') ? endpoint : `${this.baseURL}${endpoint}`;

    try {
      // Create abort controller for timeout
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), timeout);

      // Include httpOnly session cookies for OAuth-backed auth
      const headers = {
        ...fetchConfig.headers,
      };

      const response = await fetch(url, {
        ...fetchConfig,
        headers,
        signal: controller.signal,
        credentials: 'include',
      });

      clearTimeout(timeoutId);

      // Handle non-OK responses
      if (!response.ok) {
        throw await parseApiError(response);
      }

      // Parse response based on content type
      const contentType = response.headers.get('content-type');
      if (contentType && contentType.includes('application/json')) {
        return await response.json();
      }

      // Return empty object for successful requests with no content
      if (response.status === 204) {
        return {} as T;
      }

      // Return text for other content types
      return (await response.text()) as T;
    } catch (error) {
      // Handle network errors
      if (error instanceof Error && error.name === 'AbortError') {
        throw new NetworkError('Request timeout');
      }

      if (error instanceof TypeError && error.message.includes('fetch')) {
        throw new NetworkError('Network connection failed');
      }

      // Re-throw parsed API errors
      throw error;
    }
  }

  /**
   * GET request
   */
  async get<T = any>(endpoint: string, config?: RequestConfig): Promise<T> {
    return this.request<T>(endpoint, { ...config, method: 'GET' });
  }

  /**
   * POST request
   */
  async post<T = any>(
    endpoint: string,
    data?: any,
    config?: RequestConfig
  ): Promise<T> {
    return this.request<T>(endpoint, {
      ...config,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...config?.headers,
      },
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  /**
   * PUT request
   */
  async put<T = any>(
    endpoint: string,
    data?: any,
    config?: RequestConfig
  ): Promise<T> {
    return this.request<T>(endpoint, {
      ...config,
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        ...config?.headers,
      },
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  /**
   * DELETE request
   */
  async delete<T = any>(endpoint: string, config?: RequestConfig): Promise<T> {
    return this.request<T>(endpoint, { ...config, method: 'DELETE' });
  }

  /**
   * PATCH request
   */
  async patch<T = any>(
    endpoint: string,
    data?: any,
    config?: RequestConfig
  ): Promise<T> {
    return this.request<T>(endpoint, {
      ...config,
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        ...config?.headers,
      },
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  // Device Management
  async getDevices(): Promise<any[]> {
    return this.get('/devices');
  }

  async addDevice(name: string, type: string): Promise<any> {
    return this.post('/devices', { name, type });
  }

  async updateDevice(id: string, name: string, type: string): Promise<any> {
    return this.put(`/devices/${id}`, { name, type });
  }

  async deleteDevice(id: string): Promise<void> {
    return this.delete(`/devices/${id}`);
  }

  async updateDeviceStatus(
    id: string,
    status?: 'normal' | 'abnormal' | 'offline',
    temperature?: number,
    cpuUsage?: number,
    memoryUsage?: number,
    lastError?: string
  ): Promise<any> {
    return this.patch(`/devices/${id}/status`, {
      status,
      temperature,
      cpuUsage,
      memoryUsage,
      lastError,
    });
  }

  // Pairing methods
  async getAvailableDevices(): Promise<any[]> {
    return this.get('/devices/available');
  }

  async pairDevice(pairingCode: string, name: string): Promise<any> {
    return this.post('/devices/pair', { pairingCode, name });
  }
}

