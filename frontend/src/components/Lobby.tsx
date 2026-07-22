import { useState } from "react";
import { api, ApiError } from "../api/client";
import type { RoomStateView } from "../api/types";
import "./Lobby.css";

interface LobbyProps {
  state: RoomStateView;
  seatToken: string;
  onStateUpdate: (state: RoomStateView) => void;
}

export function Lobby({ state, seatToken, onStateUpdate }: LobbyProps) {
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const players = state.players ?? [];
  const me = players.find((p) => p.isYou);
  const canStart = me?.host && players.length >= 2 && players.length <= 3;

  async function handleStart() {
    setStarting(true);
    setError(null);
    try {
      const updated = await api.startGame(state.roomId!, seatToken);
      onStateUpdate(updated);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not start the game.");
    } finally {
      setStarting(false);
    }
  }

  async function handleCopyInvite() {
    const url = `${window.location.origin}/room/${state.roomId}?join=1`;
    await navigator.clipboard.writeText(url);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  return (
    <div className="lobby">
      <h2>Waiting room</h2>
      <p className="lobby__invite">
        Invite code: <strong>{state.inviteCode}</strong>
        <button type="button" onClick={handleCopyInvite} className="lobby__copy">
          {copied ? "Copied!" : "Copy invite link"}
        </button>
      </p>

      <ul className="lobby__players">
        {players.map((player) => (
          <li key={player.playerId} className="lobby__player">
            <span>{player.displayName}</span>
            {player.host && <span className="lobby__tag">Host</span>}
            {player.isYou && <span className="lobby__tag">You</span>}
          </li>
        ))}
        {players.length < 2 && (
          <li className="lobby__player lobby__player--empty">Waiting for another player…</li>
        )}
      </ul>

      {me?.host ? (
        <>
          <button
            type="button"
            className="lobby__start"
            onClick={handleStart}
            disabled={!canStart || starting}
          >
            {starting ? "Starting…" : "Start game"}
          </button>
          {!canStart && <p className="lobby__hint">Need 2-3 players to start.</p>}
        </>
      ) : (
        <p className="lobby__hint">Waiting for the host to start the game…</p>
      )}

      {error && (
        <p className="lobby__error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
