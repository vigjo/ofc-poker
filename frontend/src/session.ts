import type { SeatCredentials } from "./api/types";

const STORAGE_PREFIX = "ofc:seat:";

/**
 * Seat credentials are the only thing that identifies a guest player - there
 * is no account. Storing them per-room in localStorage is what lets a
 * refresh or a closed tab reconnect to the same seat instead of losing it.
 */
export function saveSeat(seat: SeatCredentials): void {
  if (!seat.roomId) return;
  localStorage.setItem(STORAGE_PREFIX + seat.roomId, JSON.stringify(seat));
}

export function loadSeat(roomId: string): SeatCredentials | null {
  const raw = localStorage.getItem(STORAGE_PREFIX + roomId);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as SeatCredentials;
  } catch {
    return null;
  }
}

export function clearSeat(roomId: string): void {
  localStorage.removeItem(STORAGE_PREFIX + roomId);
}
