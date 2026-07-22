import { useMemo, useState } from "react";
import { api, ApiError } from "../api/client";
import type { Card, PlacementDto, PlayerView, Row, RoomStateView } from "../api/types";
import { ROW_CAPACITY } from "../api/types";
import { PlayerBoard } from "./PlayerBoard";
import { PlayingCard } from "./PlayingCard";
import "./GameBoard.css";

interface GameBoardProps {
  state: RoomStateView;
  seatToken: string;
  onStateUpdate: (state: RoomStateView) => void;
}

interface MoveRequirements {
  placeCount: number;
  discardRequired: boolean;
}

function moveRequirements(pendingCount: number): MoveRequirements {
  if (pendingCount === 13) return { placeCount: 13, discardRequired: false };
  if (pendingCount === 5) return { placeCount: 5, discardRequired: false };
  return { placeCount: pendingCount - 1, discardRequired: true };
}

export function GameBoard({ state, seatToken, onStateUpdate }: GameBoardProps) {
  const [selectedCard, setSelectedCard] = useState<Card | null>(null);
  const [draftPlacements, setDraftPlacements] = useState<PlacementDto[]>([]);
  const [draftDiscard, setDraftDiscard] = useState<Card | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const players = state.players ?? [];
  const me = players.find((p) => p.isYou);
  const isMyTurn = Boolean(me && state.currentActor === me.playerId);
  const pending = (isMyTurn ? me?.pendingCards : null) ?? [];
  const requirements = moveRequirements(pending.length);

  const unassigned = pending.filter(
    (card) => !draftPlacements.some((p) => p.card === card) && card !== draftDiscard,
  );

  const draftByRow = useMemo(() => {
    const grouped: Partial<Record<Row, Card[]>> = {};
    for (const placement of draftPlacements) {
      grouped[placement.row] = [...(grouped[placement.row] ?? []), placement.card];
    }
    return grouped;
  }, [draftPlacements]);

  const readyToSubmit =
    isMyTurn &&
    draftPlacements.length === requirements.placeCount &&
    (!requirements.discardRequired || draftDiscard !== null);

  function resetDraft() {
    setSelectedCard(null);
    setDraftPlacements([]);
    setDraftDiscard(null);
  }

  function rowCapacityRemaining(row: Row, player: PlayerView): number {
    const placedCounts: Record<Row, number> = {
      TOP: player.top?.length ?? 0,
      MIDDLE: player.middle?.length ?? 0,
      BOTTOM: player.bottom?.length ?? 0,
    };
    const draftCount = draftByRow[row]?.length ?? 0;
    return ROW_CAPACITY[row] - placedCounts[row] - draftCount;
  }

  function handleRowClick(row: Row) {
    if (!selectedCard || !me) return;
    if (rowCapacityRemaining(row, me) <= 0) return;
    setDraftPlacements((prev) => [...prev, { card: selectedCard, row }]);
    setSelectedCard(null);
  }

  function handleUndoPlacement(row: Row, card: Card) {
    setDraftPlacements((prev) => prev.filter((p) => !(p.row === row && p.card === card)));
  }

  function handleDiscardZoneClick() {
    if (draftDiscard) {
      setDraftDiscard(null);
      return;
    }
    if (selectedCard) {
      setDraftDiscard(selectedCard);
      setSelectedCard(null);
    }
  }

  async function handleConfirm() {
    if (!readyToSubmit) return;
    setSubmitting(true);
    setError(null);
    try {
      const updated = await api.applyMove(state.roomId!, seatToken, draftPlacements, draftDiscard);
      onStateUpdate(updated);
      resetDraft();
    } catch (err) {
      setError(
        err instanceof ApiError ? err.message : "Could not submit your move. Please try again.",
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="game-board">
      <div className="game-board__opponents">
        {players
          .filter((p) => !p.isYou)
          .map((player) => (
            <PlayerBoard
              key={player.playerId}
              player={player}
              isCurrentActor={state.currentActor === player.playerId}
            />
          ))}
      </div>

      {me && (
        <div className="game-board__you">
          <PlayerBoard
            player={me}
            isCurrentActor={isMyTurn}
            size="lg"
            draftByRow={isMyTurn ? draftByRow : undefined}
            onRowClick={isMyTurn ? handleRowClick : undefined}
          />

          {isMyTurn && (
            <div className="game-board__hand" aria-label="Your dealt cards">
              <p className="game-board__hint">
                {selectedCard
                  ? "Now click a row (or the discard pile) to place it."
                  : `Select a card to place${requirements.discardRequired ? " or discard" : ""}.`}
              </p>
              <div className="game-board__hand-cards">
                {unassigned.map((card) => (
                  <PlayingCard
                    key={card}
                    card={card}
                    size="lg"
                    selected={selectedCard === card}
                    onClick={() => setSelectedCard(selectedCard === card ? null : card)}
                  />
                ))}
              </div>

              {requirements.discardRequired && (
                <button
                  type="button"
                  className="game-board__discard-zone"
                  onClick={handleDiscardZoneClick}
                  disabled={!selectedCard && !draftDiscard}
                >
                  {draftDiscard ? (
                    <>
                      Discarding <PlayingCard card={draftDiscard} size="sm" /> (click to undo)
                    </>
                  ) : (
                    "Discard pile - select a card, then click here"
                  )}
                </button>
              )}

              <div className="game-board__draft-summary">
                {draftPlacements.map((p, i) => (
                  <button
                    key={`${p.row}-${p.card}-${i}`}
                    type="button"
                    className="game-board__draft-chip"
                    onClick={() => handleUndoPlacement(p.row, p.card)}
                  >
                    {p.card} → {p.row.toLowerCase()} ✕
                  </button>
                ))}
              </div>

              {error && (
                <p className="game-board__error" role="alert">
                  {error}
                </p>
              )}

              <button
                type="button"
                className="game-board__confirm"
                disabled={!readyToSubmit || submitting}
                onClick={handleConfirm}
              >
                {submitting ? "Submitting…" : "Confirm move"}
              </button>
            </div>
          )}

          {!isMyTurn && <p className="game-board__waiting">Waiting for other players…</p>}
        </div>
      )}
    </div>
  );
}
