import type { ConnectionStatus } from "../api/ws";
import "./ConnectionBadge.css";

const LABELS: Record<ConnectionStatus, string> = {
  connecting: "Connecting…",
  open: "Live",
  reconnecting: "Reconnecting…",
  closed: "Disconnected",
};

export function ConnectionBadge({ status }: { status: ConnectionStatus }) {
  return (
    <span className={`connection-badge connection-badge--${status}`} role="status">
      <span className="connection-badge__dot" aria-hidden="true" />
      {LABELS[status]}
    </span>
  );
}
