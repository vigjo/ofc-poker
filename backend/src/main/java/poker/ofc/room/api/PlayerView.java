package poker.ofc.room.api;

import poker.ofc.domain.card.Card;

import java.util.List;

/**
 * A player's view-safe state. {@code board} rows are always populated (Open
 * Face Chinese poker boards are public the moment a card is placed).
 * {@code pendingCards} - cards dealt this street but not yet placed - is
 * only populated for the requesting viewer; for every other player only
 * {@code pendingCount} is exposed, per the "never leak another player's
 * hand" rule.
 */
public record PlayerView(String playerId, String displayName, int seatIndex, boolean host, boolean connected,
                          boolean fantasyland, List<Card> top, List<Card> middle, List<Card> bottom,
                          int pendingCount, List<Card> pendingCards, boolean isYou) {
}
