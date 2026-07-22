import type { Card, PlayerView, Row } from "../api/types";
import { ROW_CAPACITY, ROWS } from "../api/types";
import { PlayingCard } from "./PlayingCard";
import "./PlayerBoard.css";

interface PlayerBoardProps {
  player: PlayerView;
  isCurrentActor: boolean;
  size?: "sm" | "md" | "lg";
  /** Optional draft placements not yet submitted, rendered inline in their row. */
  draftByRow?: Partial<Record<Row, Card[]>>;
  onRowClick?: (row: Row) => void;
  highlightRows?: Row[];
  /** When true, show 1/2/3 hotkey badges on top/middle/bottom rows. */
  showRowHotkeys?: boolean;
}

const ROW_LABELS: Record<Row, string> = { TOP: "Top", MIDDLE: "Middle", BOTTOM: "Bottom" };
const ROW_HOTKEYS: Record<Row, string> = { TOP: "1", MIDDLE: "2", BOTTOM: "3" };

export function PlayerBoard({
  player,
  isCurrentActor,
  size = "md",
  draftByRow,
  onRowClick,
  highlightRows,
  showRowHotkeys = false,
}: PlayerBoardProps) {
  const rowCards: Record<Row, Card[]> = {
    TOP: player.top ?? [],
    MIDDLE: player.middle ?? [],
    BOTTOM: player.bottom ?? [],
  };

  return (
    <section
      className={`player-board ${isCurrentActor ? "player-board--active" : ""} ${
        player.connected === false ? "player-board--disconnected" : ""
      }`}
      aria-label={`${player.displayName}'s board`}
    >
      <header className="player-board__header">
        <span className="player-board__name">
          {player.displayName}
          {player.host ? " (host)" : ""}
        </span>
        {isCurrentActor && <span className="player-board__badge">Their turn</span>}
        {player.fantasyland && (
          <span className="player-board__badge player-board__badge--fantasy">Fantasyland</span>
        )}
        {player.connected === false && (
          <span className="player-board__badge player-board__badge--offline">Offline</span>
        )}
        {!player.isYou && (player.pendingCount ?? 0) > 0 && (
          <span className="player-board__pending">{player.pendingCount} card(s) in hand</span>
        )}
      </header>
      <div className="player-board__rows">
        {ROWS.map((row) => {
          const placed = rowCards[row];
          const draft = draftByRow?.[row] ?? [];
          const capacity = ROW_CAPACITY[row];
          const emptySlots = Math.max(0, capacity - placed.length - draft.length);
          const clickable = Boolean(onRowClick) && emptySlots > 0;
          const highlighted = highlightRows?.includes(row);

          return (
            <div
              key={row}
              className={`player-board__row ${highlighted ? "player-board__row--highlight" : ""} ${
                clickable ? "player-board__row--clickable" : ""
              }`}
              role={clickable ? "button" : undefined}
              tabIndex={clickable ? 0 : undefined}
              aria-label={clickable ? `Place selected card in ${ROW_LABELS[row]} row` : undefined}
              onClick={clickable ? () => onRowClick?.(row) : undefined}
              onKeyDown={
                clickable
                  ? (event) => {
                      if (event.key === "Enter" || event.key === " ") {
                        event.preventDefault();
                        onRowClick?.(row);
                      }
                    }
                  : undefined
              }
            >
              <span className="player-board__row-label">
                {showRowHotkeys && (
                  <span className="player-board__row-hotkey" aria-hidden="true">
                    {ROW_HOTKEYS[row]}
                  </span>
                )}
                {ROW_LABELS[row]}
              </span>
              <div className="player-board__row-cards">
                {placed.map((card, i) => (
                  <PlayingCard key={`placed-${card}-${i}`} card={card} size={size} />
                ))}
                {draft.map((card, i) => (
                  <PlayingCard key={`draft-${card}-${i}`} card={card} size={size} selected />
                ))}
                {Array.from({ length: emptySlots }).map((_, i) => (
                  <div
                    key={`empty-${i}`}
                    className={`player-board__slot player-board__slot--${size}`}
                  />
                ))}
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
