import { useEffect, useMemo, useRef, useState } from "react";
import { api, ApiError } from "../api/client";
import type { Card, PlacementDto, PlayerView, Row, RoomStateView } from "../api/types";
import { ROW_CAPACITY, ROWS } from "../api/types";
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

const ROW_BY_DIGIT: Record<string, Row> = {
  "1": "TOP",
  "2": "MIDDLE",
  "3": "BOTTOM",
};

function moveRequirements(pendingCount: number): MoveRequirements {
  if (pendingCount === 13) return { placeCount: 13, discardRequired: false };
  if (pendingCount === 5) return { placeCount: 5, discardRequired: false };
  return { placeCount: pendingCount - 1, discardRequired: true };
}

function isTypingTarget(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) return false;
  const tag = target.tagName;
  return tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT" || target.isContentEditable;
}

/** Map a digit key to a 1-based hand index. `0` selects the 10th card. */
function digitToHandIndex(key: string): number | null {
  if (key >= "1" && key <= "9") return Number(key);
  if (key === "0") return 10;
  return null;
}

function hotkeyBadgeForIndex(index: number): string | undefined {
  if (index < 9) return String(index + 1);
  if (index === 9) return "0";
  return undefined;
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
  const canAct = Boolean(
    me && (isMyTurn || (me.fantasyland && (me.pendingCards?.length ?? 0) > 0)),
  );
  const pending = (canAct ? me?.pendingCards : null) ?? [];
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

  const inferredDiscard =
    requirements.discardRequired &&
    draftPlacements.length === requirements.placeCount &&
    draftDiscard == null &&
    unassigned.length === 1
      ? unassigned[0]!
      : null;

  const discardForSubmit = draftDiscard ?? inferredDiscard;

  const readyToSubmit =
    canAct &&
    draftPlacements.length === requirements.placeCount &&
    (!requirements.discardRequired || discardForSubmit != null);

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

  function placeCardOnRow(card: Card, row: Row) {
    if (!me) return;
    if (rowCapacityRemaining(row, me) <= 0) return;
    setDraftPlacements((prev) => [...prev, { card, row }]);
    setSelectedCard(null);
  }

  function handleRowClick(row: Row) {
    if (!selectedCard) return;
    placeCardOnRow(selectedCard, row);
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

    let discard: Card | null = null;
    if (requirements.discardRequired) {
      const leftovers = pending.filter((card) => !draftPlacements.some((p) => p.card === card));
      if (leftovers.length !== 1) {
        setError("Place the required cards; the remaining card will be discarded automatically.");
        return;
      }
      discard = leftovers[0]!;
    }

    setSubmitting(true);
    setError(null);
    try {
      const updated = await api.applyMove(state.roomId!, seatToken, draftPlacements, discard);
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

  const hotkeysRef = useRef({
    canAct,
    submitting,
    selectedCard,
    unassigned,
    readyToSubmit,
    me,
    placeCardOnRow,
    handleConfirm,
  });
  hotkeysRef.current = {
    canAct,
    submitting,
    selectedCard,
    unassigned,
    readyToSubmit,
    me,
    placeCardOnRow,
    handleConfirm,
  };

  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      const ctx = hotkeysRef.current;
      if (!ctx.canAct || ctx.submitting) return;
      if (event.defaultPrevented || event.metaKey || event.ctrlKey || event.altKey) return;
      if (isTypingTarget(event.target)) return;

      if (event.key === "Escape") {
        event.preventDefault();
        setSelectedCard(null);
        return;
      }

      if (event.key === "Backspace" || event.key === "Delete") {
        event.preventDefault();
        setDraftPlacements((prev) => (prev.length === 0 ? prev : prev.slice(0, -1)));
        return;
      }

      if (event.key === "Enter") {
        if (!ctx.readyToSubmit) return;
        event.preventDefault();
        void ctx.handleConfirm();
        return;
      }

      const handIndex = digitToHandIndex(event.key);
      if (handIndex == null) return;

      if (ctx.selectedCard) {
        const row = ROW_BY_DIGIT[event.key];
        if (!row) return;
        event.preventDefault();
        ctx.placeCardOnRow(ctx.selectedCard, row);
        return;
      }

      const card = ctx.unassigned[handIndex - 1];
      if (!card) return;
      event.preventDefault();
      setSelectedCard(card);
    }

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, []);

  const discardPreview = draftDiscard ?? inferredDiscard;
  const showRowHotkeys = Boolean(selectedCard);

  const hintText = selectedCard
    ? "Press 1 / 2 / 3 for top / middle / bottom, or click a row. Esc clears selection."
    : requirements.discardRequired
      ? "Press a number to select a card, then 1–3 for a row. Leftover discards automatically. Backspace undoes · Enter confirms."
      : "Press a number to select a card, then 1–3 for a row. Backspace undoes · Enter confirms.";

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
            draftByRow={canAct ? draftByRow : undefined}
            onRowClick={canAct ? handleRowClick : undefined}
            highlightRows={
              showRowHotkeys && me
                ? ROWS.filter((row) => rowCapacityRemaining(row, me) > 0)
                : undefined
            }
            showRowHotkeys={showRowHotkeys}
          />

          {canAct && (
            <div className="game-board__hand" aria-label="Your dealt cards">
              <p className="game-board__hint">{hintText}</p>
              <div className="game-board__hand-cards">
                {unassigned.map((card, index) => (
                  <PlayingCard
                    key={card}
                    card={card}
                    size="lg"
                    selected={selectedCard === card}
                    hotkeyBadge={hotkeyBadgeForIndex(index)}
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
                  {discardPreview ? (
                    <>
                      {draftDiscard ? "Discarding" : "Will discard"}{" "}
                      <PlayingCard card={discardPreview} size="sm" />
                      {draftDiscard ? " (click to undo)" : ""}
                    </>
                  ) : (
                    "Leftover card discards automatically — or select a card and click here"
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
                onClick={() => void handleConfirm()}
              >
                {submitting ? "Submitting…" : "Confirm move"}
              </button>
            </div>
          )}

          {!canAct && <p className="game-board__waiting">Waiting for other players…</p>}
        </div>
      )}
    </div>
  );
}
