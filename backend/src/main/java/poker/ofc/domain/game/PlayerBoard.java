package poker.ofc.domain.game;

import poker.ofc.domain.board.Board;
import poker.ofc.domain.card.Card;

import java.util.ArrayList;
import java.util.List;

/**
 * One player's state within a single hand: their board plus whatever cards
 * they currently hold and have not yet placed or discarded.
 */
public final class PlayerBoard {

    private final String playerId;
    private final Board board;
    private final List<Card> pendingCards = new ArrayList<>();
    private final boolean fantasyland;

    public PlayerBoard(String playerId, boolean fantasyland) {
        this(playerId, fantasyland, new Board(), List.of());
    }

    /** Rebuilds a player's in-progress state from a previously persisted snapshot. */
    public PlayerBoard(String playerId, boolean fantasyland, Board board, List<Card> pendingCards) {
        this.playerId = playerId;
        this.fantasyland = fantasyland;
        this.board = board;
        this.pendingCards.addAll(pendingCards);
    }

    public String playerId() {
        return playerId;
    }

    public Board board() {
        return board;
    }

    public boolean isFantasyland() {
        return fantasyland;
    }

    public List<Card> pendingCards() {
        return List.copyOf(pendingCards);
    }

    void receiveCards(List<Card> cards) {
        if (!pendingCards.isEmpty()) {
            throw new IllegalStateException(playerId + " still has unplaced cards from the previous street");
        }
        pendingCards.addAll(cards);
    }

    void clearPending() {
        pendingCards.clear();
    }

    boolean hasPendingCards() {
        return !pendingCards.isEmpty();
    }
}
