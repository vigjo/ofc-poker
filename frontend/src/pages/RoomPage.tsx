import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api, ApiError } from "../api/client";
import type { RoomStateView } from "../api/types";
import { RoomSocket, type ConnectionStatus } from "../api/ws";
import { clearSeat, loadSeat, saveSeat } from "../session";
import { ConnectionBadge } from "../components/ConnectionBadge";
import { Lobby } from "../components/Lobby";
import { GameBoard } from "../components/GameBoard";
import { HandResultView } from "../components/HandResultView";
import "./RoomPage.css";

export function RoomPage() {
  const { roomId = "" } = useParams();
  const navigate = useNavigate();
  const [seatToken, setSeatToken] = useState<string | null>(null);
  const [displayName, setDisplayName] = useState("");
  const [joining, setJoining] = useState(false);
  const [joinError, setJoinError] = useState<string | null>(null);
  const [state, setState] = useState<RoomStateView | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>("connecting");
  const socketRef = useRef<RoomSocket | null>(null);

  useEffect(() => {
    const seat = loadSeat(roomId);
    setSeatToken(seat?.seatToken ?? null);
  }, [roomId]);

  useEffect(() => {
    if (!seatToken) return;

    let cancelled = false;
    api
      .getState(roomId, seatToken)
      .then((initial) => {
        if (!cancelled) setState(initial);
      })
      .catch((err) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 401) {
          clearSeat(roomId);
          setSeatToken(null);
          return;
        }
        setLoadError(err instanceof ApiError ? err.message : "Could not load this room.");
      });

    const socket = new RoomSocket(roomId, seatToken, {
      onState: (next) => !cancelled && setState(next),
      onStatusChange: (status) => !cancelled && setConnectionStatus(status),
    });
    socket.connect();
    socketRef.current = socket;

    return () => {
      cancelled = true;
      socket.close();
      socketRef.current = null;
    };
  }, [roomId, seatToken]);

  async function handleJoinSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!displayName.trim()) {
      setJoinError("Enter a display name.");
      return;
    }
    setJoining(true);
    setJoinError(null);
    try {
      const seat = await api.joinRoom(roomId, displayName.trim());
      saveSeat(seat);
      setSeatToken(seat.seatToken ?? null);
    } catch (err) {
      setJoinError(err instanceof ApiError ? err.message : "Could not join this room.");
    } finally {
      setJoining(false);
    }
  }

  if (!seatToken) {
    return (
      <div className="room-page room-page--join">
        <h1>Join room {roomId}</h1>
        <form className="room-page__join-form" onSubmit={handleJoinSubmit}>
          <label>
            <span>Display name</span>
            <input
              type="text"
              value={displayName}
              maxLength={24}
              onChange={(e) => setDisplayName(e.target.value)}
              autoFocus
            />
          </label>
          {joinError && (
            <p className="room-page__error" role="alert">
              {joinError}
            </p>
          )}
          <button type="submit" disabled={joining}>
            {joining ? "Joining…" : "Join room"}
          </button>
        </form>
        <button type="button" className="room-page__link-button" onClick={() => navigate("/")}>
          ← Back home
        </button>
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="room-page">
        <p className="room-page__error" role="alert">
          {loadError}
        </p>
        <button type="button" className="room-page__link-button" onClick={() => navigate("/")}>
          ← Back home
        </button>
      </div>
    );
  }

  if (!state) {
    return (
      <div className="room-page">
        <p>Loading room…</p>
      </div>
    );
  }

  return (
    <div className="room-page">
      <header className="room-page__header">
        <div>
          <h1>Room {state.inviteCode ?? roomId}</h1>
        </div>
        <ConnectionBadge status={connectionStatus} />
      </header>

      {state.status === "WAITING" && (
        <Lobby state={state} seatToken={seatToken} onStateUpdate={setState} />
      )}
      {state.status === "IN_PROGRESS" && (
        <GameBoard state={state} seatToken={seatToken} onStateUpdate={setState} />
      )}
      {state.status === "HAND_COMPLETE" && state.lastHandResult && (
        <HandResultView
          state={state}
          result={state.lastHandResult}
          seatToken={seatToken}
          onStateUpdate={setState}
        />
      )}
      {state.status === "ABANDONED" && (
        <div className="room-page__abandoned">
          <p>This room has been abandoned and is no longer active.</p>
          <button type="button" className="room-page__link-button" onClick={() => navigate("/")}>
            ← Back home
          </button>
        </div>
      )}
    </div>
  );
}
