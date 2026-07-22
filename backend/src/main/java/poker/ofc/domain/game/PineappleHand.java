package poker.ofc.domain.game;

import poker.ofc.domain.card.Card;
import poker.ofc.domain.card.Deck;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * A single dealt hand of Pineapple OFC for 2-3 players sharing one deck.
 *
 * <p>Non-fantasyland players act in a fixed turn order within each street so
 * that, faithful to real open-face rules, a player can see every board that
 * has already acted this street before placing their own cards. Fantasyland
 * players receive all 13 cards immediately and may submit their single
 * whole-board move at any time, independent of the turn order.
 */
public final class PineappleHand {

    private final List<String> seatOrder;
    private final List<String> activeOrder;
    private final Map<String, PlayerBoard> boards;
    private final Deck deck;
    private Street street;
    private int turnPointer;

    private PineappleHand(List<String> seatOrder, List<String> activeOrder,
                           Map<String, PlayerBoard> boards, Deck deck) {
        this.seatOrder = seatOrder;
        this.activeOrder = activeOrder;
        this.boards = boards;
        this.deck = deck;
        this.street = Street.INITIAL;
        this.turnPointer = 0;
    }

    public static PineappleHand deal(List<String> seatOrder, Set<String> fantasylandPlayerIds, Random random) {
        if (seatOrder.size() < 2 || seatOrder.size() > 3) {
            throw new IllegalArgumentException("Pineapple OFC supports 2-3 players, got " + seatOrder.size());
        }
        Deck deck = Deck.shuffled(random);
        Map<String, PlayerBoard> boards = new LinkedHashMap<>();
        List<String> activeOrder = new ArrayList<>();

        for (String playerId : seatOrder) {
            boolean fantasyland = fantasylandPlayerIds.contains(playerId);
            PlayerBoard board = new PlayerBoard(playerId, fantasyland);
            if (fantasyland) {
                board.receiveCards(deck.deal(13));
            } else {
                board.receiveCards(deck.deal(Street.INITIAL.dealCount()));
                activeOrder.add(playerId);
            }
            boards.put(playerId, board);
        }

        PineappleHand hand = new PineappleHand(List.copyOf(seatOrder), activeOrder, boards, deck);
        if (activeOrder.isEmpty()) {
            hand.street = Street.COMPLETE;
        }
        return hand;
    }

    /** Rebuilds a hand from a previously persisted snapshot (e.g. after an API restart). */
    public static PineappleHand restore(List<String> seatOrder, List<String> activeOrder,
                                         Map<String, PlayerBoard> boards, List<Card> remainingDeck,
                                         Street street, int turnPointer) {
        PineappleHand hand = new PineappleHand(List.copyOf(seatOrder), List.copyOf(activeOrder),
                new LinkedHashMap<>(boards), Deck.fromRemaining(remainingDeck));
        hand.street = street;
        hand.turnPointer = turnPointer;
        return hand;
    }

    public Street street() {
        return street;
    }

    public int turnPointer() {
        return turnPointer;
    }

    public List<String> activeOrder() {
        return List.copyOf(activeOrder);
    }

    public List<Card> remainingDeckCards() {
        return deck.peekRemaining();
    }

    public boolean isComplete() {
        return boards.values().stream().allMatch(b -> b.board().isComplete());
    }

    public PlayerBoard boardOf(String playerId) {
        PlayerBoard board = boards.get(playerId);
        if (board == null) {
            throw new IllegalArgumentException("Unknown player: " + playerId);
        }
        return board;
    }

    public List<String> seatOrder() {
        return seatOrder;
    }

    /** Null once the street's dealing/turn cycle has ended for non-fantasyland players. */
    public String currentActor() {
        if (activeOrder.isEmpty() || street == Street.COMPLETE) {
            return null;
        }
        return activeOrder.get(turnPointer);
    }

    public void applyMove(TurnMove move) {
        PlayerBoard board = boardOf(move.playerId());
        if (board.isFantasyland()) {
            applyFantasylandMove(board, move);
        } else {
            applyStreetMove(board, move);
        }
    }

    private void applyFantasylandMove(PlayerBoard board, TurnMove move) {
        if (!board.hasPendingCards()) {
            throw new IllegalMoveException(board.playerId() + " has already completed their fantasyland board");
        }
        if (move.discard() != null) {
            throw new IllegalMoveException("Fantasyland moves do not discard");
        }
        if (move.placements().size() != 13) {
            throw new IllegalMoveException("Fantasyland moves must place exactly 13 cards");
        }
        commitPlacements(board, move.placements(), null);
    }

    private void applyStreetMove(PlayerBoard board, TurnMove move) {
        String expectedActor = currentActor();
        if (!board.playerId().equals(expectedActor)) {
            throw new IllegalMoveException("It is not " + move.playerId() + "'s turn");
        }
        if (move.placements().size() != street.placeCount()) {
            throw new IllegalMoveException(
                    "Street " + street + " requires placing " + street.placeCount() + " cards");
        }
        boolean discardRequired = street.discardCount() == 1;
        if (discardRequired == (move.discard() == null)) {
            throw new IllegalMoveException(
                    discardRequired ? "Street " + street + " requires a discard" : "Street " + street + " has no discard");
        }

        commitPlacements(board, move.placements(), move.discard());
        advanceTurn();
    }

    private void commitPlacements(PlayerBoard board, List<Placement> placements, Card discard) {
        List<Card> pending = new ArrayList<>(board.pendingCards());
        List<Card> used = new ArrayList<>();
        for (Placement placement : placements) {
            if (!pending.remove(placement.card())) {
                throw new IllegalMoveException(board.playerId() + " was not dealt " + placement.card());
            }
            used.add(placement.card());
        }
        if (discard != null) {
            if (!pending.remove(discard)) {
                throw new IllegalMoveException(board.playerId() + " was not dealt " + discard);
            }
        }
        if (!pending.isEmpty()) {
            throw new IllegalMoveException(board.playerId() + " must place or discard every dealt card");
        }
        for (Placement placement : placements) {
            if (board.board().remainingCapacity(placement.row()) <= 0) {
                throw new IllegalMoveException("Row " + placement.row() + " is already full");
            }
            board.board().place(placement.row(), placement.card());
        }
        board.clearPending();
    }

    private void advanceTurn() {
        turnPointer++;
        if (turnPointer < activeOrder.size()) {
            return;
        }
        turnPointer = 0;
        street = street.next();
        if (street == Street.COMPLETE) {
            return;
        }
        for (String playerId : activeOrder) {
            boards.get(playerId).receiveCards(deck.deal(street.dealCount()));
        }
    }
}
