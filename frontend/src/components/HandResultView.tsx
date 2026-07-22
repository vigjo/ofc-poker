import { useState } from "react";
import { api, ApiError } from "../api/client";
import type { HandResult, PlayerView, RoomStateView } from "../api/types";
import "./HandResultView.css";

interface HandResultViewProps {
  state: RoomStateView;
  result: HandResult;
  seatToken: string;
  onStateUpdate: (state: RoomStateView) => void;
}

function playerName(players: PlayerView[], playerId?: string): string {
  return players.find((p) => p.playerId === playerId)?.displayName ?? "Unknown";
}

export function HandResultView({ state, result, seatToken, onStateUpdate }: HandResultViewProps) {
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const players = state.players ?? [];
  const me = players.find((p) => p.isYou);
  const rows = result.players ?? [];

  async function handleNextHand() {
    setStarting(true);
    setError(null);
    try {
      const updated = await api.startNextHand(state.roomId!, seatToken);
      onStateUpdate(updated);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not start the next hand.");
    } finally {
      setStarting(false);
    }
  }

  return (
    <div className="hand-result">
      <h2>Hand complete</h2>
      <table className="hand-result__table">
        <thead>
          <tr>
            <th>Player</th>
            <th>Fouled</th>
            <th>Royalties</th>
            <th>Total</th>
            <th>Running total</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr
              key={row.playerId}
              className={row.total && row.total > 0 ? "hand-result__winner" : undefined}
            >
              <td>{playerName(players, row.playerId)}</td>
              <td>{row.fouled ? "Yes" : "—"}</td>
              <td>{row.royalties ?? 0}</td>
              <td>{(row.total ?? 0) > 0 ? `+${row.total}` : (row.total ?? 0)}</td>
              <td>{state.runningTotals?.[row.playerId ?? ""] ?? 0}</td>
            </tr>
          ))}
        </tbody>
      </table>

      {rows.some((r) => r.fantasylandNextHand) && (
        <p className="hand-result__fantasy">
          {rows
            .filter((r) => r.fantasylandNextHand)
            .map((r) => playerName(players, r.playerId))
            .join(", ")}{" "}
          qualified for Fantasyland next hand!
        </p>
      )}

      {me?.host ? (
        <button
          type="button"
          className="hand-result__next"
          onClick={handleNextHand}
          disabled={starting}
        >
          {starting ? "Starting…" : "Start next hand"}
        </button>
      ) : (
        <p className="hand-result__hint">Waiting for the host to start the next hand…</p>
      )}

      {error && (
        <p className="hand-result__error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
