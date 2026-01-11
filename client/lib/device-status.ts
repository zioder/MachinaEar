import { Device } from "@/types/api";

export const OFFLINE_THRESHOLD_MS = 30_000;

const normalizeStatus = (status?: string) =>
  (status ?? "").toString().trim().toLowerCase();

const parseTimeMs = (value?: string) => {
  if (!value) return null;
  const ms = new Date(value).getTime();
  return Number.isFinite(ms) ? ms : null;
};

/**
 * A device is considered offline if:
 * - backend explicitly marks it offline, OR
 * - backend says isOnline=false, OR
 * - no heartbeat, OR
 * - lastHeartbeat is older than OFFLINE_THRESHOLD_MS, OR
 * - lastHeartbeat is present but unparsable (safer to treat as offline)
 */
export function isDeviceOffline(device: Device, nowMs: number = Date.now()): boolean {
  const status = normalizeStatus(device.status as unknown as string);
  if (status === "offline") return true;
  if (device.isOnline === false) return true;

  if (!device.lastHeartbeat) return true;
  const heartbeatMs = parseTimeMs(device.lastHeartbeat);
  if (heartbeatMs === null) return true;

  return nowMs - heartbeatMs > OFFLINE_THRESHOLD_MS;
}

export function getDeviceEffectiveStatus(
  device: Device,
  nowMs: number = Date.now()
): "normal" | "abnormal" | "offline" | "unknown" {
  if (isDeviceOffline(device, nowMs)) return "offline";

  const status = normalizeStatus(device.status as unknown as string);
  if (status === "normal" || status === "abnormal") return status;

  // Treat any other server status as unknown for UI grouping
  return "unknown";
}

