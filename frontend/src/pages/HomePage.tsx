import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, ApiError } from "../api/client";
import { saveSeat } from "../session";
import "./HomePage.css";

export function HomePage() {
  const navigate = useNavigate();
  const [displayName, setDisplayName] = useState("");
  const [inviteCode, setInviteCode] = useState("");
  const [mode, setMode] = useState<"create" | "join">("create");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!displayName.trim()) {
      setError("Enter a display name.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const seat =
        mode === "create"
          ? await api.createRoom(displayName.trim())
          : await api.joinRoom(inviteCode.trim().toUpperCase(), displayName.trim());
      saveSeat(seat);
      navigate(`/room/${seat.roomId}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Something went wrong. Please try again.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="home-page">
      <h1>Pineapple OFC</h1>
      <p className="home-page__tagline">
        Private, invite-only Open Face Chinese Poker for 2-3 players.
      </p>

      <div className="home-page__tabs" role="tablist">
        <button
          type="button"
          role="tab"
          aria-selected={mode === "create"}
          className={mode === "create" ? "home-page__tab home-page__tab--active" : "home-page__tab"}
          onClick={() => setMode("create")}
        >
          Create a room
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={mode === "join"}
          className={mode === "join" ? "home-page__tab home-page__tab--active" : "home-page__tab"}
          onClick={() => setMode("join")}
        >
          Join with a code
        </button>
      </div>

      <form className="home-page__form" onSubmit={handleSubmit}>
        <label className="home-page__field">
          <span>Display name</span>
          <input
            type="text"
            value={displayName}
            maxLength={24}
            onChange={(e) => setDisplayName(e.target.value)}
            placeholder="e.g. Alex"
            autoFocus
          />
        </label>

        {mode === "join" && (
          <label className="home-page__field">
            <span>Invite code</span>
            <input
              type="text"
              value={inviteCode}
              maxLength={6}
              onChange={(e) => setInviteCode(e.target.value)}
              placeholder="e.g. 7K3PXQ"
              className="home-page__code-input"
            />
          </label>
        )}

        {error && (
          <p className="home-page__error" role="alert">
            {error}
          </p>
        )}

        <button type="submit" className="home-page__submit" disabled={busy}>
          {busy ? "Please wait…" : mode === "create" ? "Create room" : "Join room"}
        </button>
      </form>
    </div>
  );
}
