import type { Card } from "../api/types";
import "./PlayingCard.css";

const SUIT_SYMBOLS: Record<string, string> = { h: "♥", d: "♦", c: "♣", s: "♠" };
const SUIT_COLORS: Record<string, string> = {
  h: "red",
  d: "red",
  c: "black",
  s: "black",
};
const RANK_LABELS: Record<string, string> = { T: "10" };

interface PlayingCardProps {
  card?: Card | null;
  faceDown?: boolean;
  selected?: boolean;
  size?: "sm" | "md" | "lg";
  onClick?: () => void;
  label?: string;
}

export function PlayingCard({
  card,
  faceDown,
  selected,
  size = "md",
  onClick,
  label,
}: PlayingCardProps) {
  const interactive = Boolean(onClick);

  if (faceDown || !card) {
    return (
      <div
        className={`playing-card playing-card--${size} playing-card--back`}
        role={interactive ? "button" : undefined}
        aria-label={label ?? "Face-down card"}
      />
    );
  }

  const rank = card.slice(0, -1);
  const suit = card.slice(-1);
  const rankLabel = RANK_LABELS[rank] ?? rank;
  const color = SUIT_COLORS[suit] ?? "black";

  return (
    <button
      type="button"
      className={`playing-card playing-card--${size} playing-card--${color} ${
        selected ? "playing-card--selected" : ""
      }`}
      onClick={onClick}
      disabled={!interactive}
      aria-pressed={interactive ? selected : undefined}
      aria-label={label ?? `${rankLabel} of ${suitName(suit)}`}
    >
      <span className="playing-card__corner playing-card__corner--top">
        {rankLabel}
        {SUIT_SYMBOLS[suit]}
      </span>
      <span className="playing-card__pip">{SUIT_SYMBOLS[suit]}</span>
      <span className="playing-card__corner playing-card__corner--bottom">
        {rankLabel}
        {SUIT_SYMBOLS[suit]}
      </span>
    </button>
  );
}

function suitName(suit: string): string {
  return { h: "hearts", d: "diamonds", c: "clubs", s: "spades" }[suit] ?? suit;
}
