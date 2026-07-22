import type { Card, MoveRequest, PlacementDto, RoomStateView, SeatCredentials } from "./types";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...init.headers,
    },
  });

  if (!response.ok) {
    let message = response.statusText;
    try {
      const body = (await response.json()) as { message?: string };
      message = body.message ?? message;
    } catch {
      // Response had no JSON body; fall back to the status text.
    }
    throw new ApiError(message, response.status);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

function authHeaders(seatToken: string): HeadersInit {
  return { Authorization: `Bearer ${seatToken}` };
}

export const api = {
  createRoom(displayName: string): Promise<SeatCredentials> {
    return request("/api/rooms", {
      method: "POST",
      body: JSON.stringify({ displayName }),
    });
  },

  joinRoom(roomId: string, displayName: string): Promise<SeatCredentials> {
    return request(`/api/rooms/${roomId}/join`, {
      method: "POST",
      body: JSON.stringify({ displayName }),
    });
  },

  startGame(roomId: string, seatToken: string): Promise<RoomStateView> {
    return request(`/api/rooms/${roomId}/start`, {
      method: "POST",
      headers: authHeaders(seatToken),
    });
  },

  startNextHand(roomId: string, seatToken: string): Promise<RoomStateView> {
    return request(`/api/rooms/${roomId}/next-hand`, {
      method: "POST",
      headers: authHeaders(seatToken),
    });
  },

  getState(roomId: string, seatToken: string): Promise<RoomStateView> {
    return request(`/api/rooms/${roomId}/state`, {
      headers: authHeaders(seatToken),
    });
  },

  applyMove(
    roomId: string,
    seatToken: string,
    placements: PlacementDto[],
    discard: Card | null,
  ): Promise<RoomStateView> {
    const body: MoveRequest = {
      requestId: crypto.randomUUID(),
      placements,
      ...(discard ? { discard } : {}),
    };
    return request(`/api/rooms/${roomId}/moves`, {
      method: "POST",
      headers: authHeaders(seatToken),
      body: JSON.stringify(body),
    });
  },
};
