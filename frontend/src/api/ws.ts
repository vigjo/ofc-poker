import type { RoomStateView } from "./types";

const WS_BASE_URL = import.meta.env.VITE_WS_BASE_URL ?? "ws://localhost:8080";

export type ConnectionStatus = "connecting" | "open" | "reconnecting" | "closed";

interface RoomSocketHandlers {
  onState: (state: RoomStateView) => void;
  onStatusChange: (status: ConnectionStatus) => void;
}

const HEARTBEAT_INTERVAL_MS = 25_000;
const MAX_BACKOFF_MS = 10_000;

/**
 * Reconnecting WebSocket client for one room's state stream. Moves
 * themselves always go through the REST API (see api/client.ts); this
 * socket only receives server-pushed state and sends lightweight "ping"
 * heartbeats to keep the connection (and the ALB/Fargate idle-connection
 * detector) alive.
 */
export class RoomSocket {
  private socket: WebSocket | null = null;
  private heartbeat: ReturnType<typeof setInterval> | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private reconnectAttempt = 0;
  private closedByCaller = false;
  private readonly roomId: string;
  private readonly seatToken: string;
  private readonly handlers: RoomSocketHandlers;

  constructor(roomId: string, seatToken: string, handlers: RoomSocketHandlers) {
    this.roomId = roomId;
    this.seatToken = seatToken;
    this.handlers = handlers;
  }

  connect(): void {
    this.closedByCaller = false;
    this.open();
  }

  close(): void {
    this.closedByCaller = true;
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
    if (this.heartbeat) clearInterval(this.heartbeat);
    this.socket?.close();
    this.socket = null;
  }

  private open(): void {
    this.handlers.onStatusChange(this.reconnectAttempt === 0 ? "connecting" : "reconnecting");
    const url = `${WS_BASE_URL}/ws/rooms/${this.roomId}?token=${encodeURIComponent(this.seatToken)}`;
    const socket = new WebSocket(url);
    this.socket = socket;

    socket.onopen = () => {
      this.reconnectAttempt = 0;
      this.handlers.onStatusChange("open");
      this.heartbeat = setInterval(() => socket.send("ping"), HEARTBEAT_INTERVAL_MS);
    };

    socket.onmessage = (event: MessageEvent<string>) => {
      try {
        this.handlers.onState(JSON.parse(event.data) as RoomStateView);
      } catch {
        // Ignore malformed frames rather than tearing down the connection.
      }
    };

    socket.onclose = () => {
      if (this.heartbeat) clearInterval(this.heartbeat);
      if (this.closedByCaller) {
        this.handlers.onStatusChange("closed");
        return;
      }
      this.scheduleReconnect();
    };

    socket.onerror = () => socket.close();
  }

  private scheduleReconnect(): void {
    this.handlers.onStatusChange("reconnecting");
    const delay = Math.min(MAX_BACKOFF_MS, 500 * 2 ** this.reconnectAttempt);
    this.reconnectAttempt += 1;
    this.reconnectTimer = setTimeout(() => this.open(), delay);
  }
}
