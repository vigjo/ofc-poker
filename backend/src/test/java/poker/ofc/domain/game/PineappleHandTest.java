package poker.ofc.domain.game;

import org.junit.jupiter.api.Test;
import poker.ofc.domain.board.Row;
import poker.ofc.domain.card.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PineappleHandTest {

    private static void playFullHand(PineappleHand hand, List<String> activePlayers) {
        while (!hand.isComplete()) {
            for (String playerId : activePlayers) {
                PlayerBoard board = hand.boardOf(playerId);
                if (board.board().isComplete()) {
                    continue;
                }
                if (!board.isFantasyland() && !playerId.equals(hand.currentActor())) {
                    continue;
                }
                hand.applyMove(planMove(board));
            }
        }
    }

    private static TurnMove planMove(PlayerBoard board) {
        List<Card> pending = board.pendingCards();
        boolean discardNeeded = !board.isFantasyland() && pending.size() > requiredPlacements(board);
        List<Card> toPlace = new ArrayList<>(pending);
        Card discard = discardNeeded ? toPlace.remove(toPlace.size() - 1) : null;

        int topRoom = board.board().remainingCapacity(Row.TOP);
        int middleRoom = board.board().remainingCapacity(Row.MIDDLE);
        List<Placement> placements = new ArrayList<>();
        for (Card card : toPlace) {
            Row row;
            if (topRoom > 0) {
                row = Row.TOP;
                topRoom--;
            } else if (middleRoom > 0) {
                row = Row.MIDDLE;
                middleRoom--;
            } else {
                row = Row.BOTTOM;
            }
            placements.add(new Placement(card, row));
        }
        return new TurnMove(board.playerId(), placements, discard);
    }

    private static int requiredPlacements(PlayerBoard board) {
        if (board.isFantasyland()) {
            return 13;
        }
        int placed = board.board().cardCount();
        return placed == 0 ? 5 : 2;
    }

    @Test
    void dealsFiveCardsToEachPlayerInitially() {
        PineappleHand hand = PineappleHand.deal(List.of("alice", "bob"), Set.of(), new Random(1));
        assertEquals(5, hand.boardOf("alice").pendingCards().size());
        assertEquals(5, hand.boardOf("bob").pendingCards().size());
        assertEquals("alice", hand.currentActor());
    }

    @Test
    void enforcesTurnOrderWithinAStreet() {
        PineappleHand hand = PineappleHand.deal(List.of("alice", "bob"), Set.of(), new Random(2));
        TurnMove bobMove = planMove(hand.boardOf("bob"));
        IllegalMoveException ex = assertThrows(IllegalMoveException.class, () -> hand.applyMove(bobMove));
        assertTrue(ex.getMessage().contains("not"));
    }

    @Test
    void turnPassesToNextPlayerAfterAMove() {
        PineappleHand hand = PineappleHand.deal(List.of("alice", "bob"), Set.of(), new Random(3));
        hand.applyMove(planMove(hand.boardOf("alice")));
        assertEquals("bob", hand.currentActor());
    }

    @Test
    void streetAdvancesAndDealsNewCardsOnceEveryoneHasActed() {
        PineappleHand hand = PineappleHand.deal(List.of("alice", "bob"), Set.of(), new Random(4));
        hand.applyMove(planMove(hand.boardOf("alice")));
        hand.applyMove(planMove(hand.boardOf("bob")));
        assertEquals(Street.STREET_1, hand.street());
        assertEquals(3, hand.boardOf("alice").pendingCards().size());
        assertEquals(3, hand.boardOf("bob").pendingCards().size());
        assertEquals("alice", hand.currentActor(), "turn order repeats each street");
    }

    @Test
    void rejectsWrongPlacementCount() {
        PineappleHand hand = PineappleHand.deal(List.of("alice", "bob"), Set.of(), new Random(5));
        List<Card> pending = hand.boardOf("alice").pendingCards();
        TurnMove tooFew = new TurnMove("alice", List.of(new Placement(pending.get(0), Row.TOP)), null);
        assertThrows(IllegalMoveException.class, () -> hand.applyMove(tooFew));
    }

    @Test
    void rejectsPlacingACardThatWasNotDealt() {
        PineappleHand hand = PineappleHand.deal(List.of("alice", "bob"), Set.of(), new Random(6));
        List<Card> pending = hand.boardOf("alice").pendingCards();
        Card foreign = allCards().stream().filter(c -> !pending.contains(c)).findFirst().orElseThrow();
        List<Placement> placements = new ArrayList<>();
        placements.add(new Placement(foreign, Row.TOP));
        for (int i = 1; i < 5; i++) {
            placements.add(new Placement(pending.get(i), Row.TOP));
        }
        TurnMove move = new TurnMove("alice", placements, null);
        assertThrows(IllegalMoveException.class, () -> hand.applyMove(move));
    }

    @Test
    void rejectsOverfillingARow() {
        PineappleHand hand = PineappleHand.deal(List.of("alice", "bob"), Set.of(), new Random(8));
        List<Card> pending = hand.boardOf("alice").pendingCards();
        List<Placement> placements = pending.stream().map(c -> new Placement(c, Row.TOP)).toList();
        TurnMove move = new TurnMove("alice", placements, null);
        assertThrows(IllegalMoveException.class, () -> hand.applyMove(move));
    }

    @Test
    void discardIsRejectedOnTheDiscardFreeInitialStreet() {
        PineappleHand hand = PineappleHand.deal(List.of("alice", "bob"), Set.of(), new Random(9));
        List<Card> pending = hand.boardOf("alice").pendingCards();
        List<Placement> placements = List.of(
                new Placement(pending.get(0), Row.TOP),
                new Placement(pending.get(1), Row.TOP),
                new Placement(pending.get(2), Row.TOP),
                new Placement(pending.get(3), Row.MIDDLE),
                new Placement(pending.get(4), Row.MIDDLE));
        TurnMove withUnexpectedDiscard = new TurnMove("alice", placements, pending.get(4));
        assertThrows(IllegalMoveException.class, () -> hand.applyMove(withUnexpectedDiscard));
    }

    @Test
    void discardIsRequiredOnStreetOne() {
        PineappleHand hand = PineappleHand.deal(List.of("alice", "bob"), Set.of(), new Random(13));
        hand.applyMove(planMove(hand.boardOf("alice")));
        hand.applyMove(planMove(hand.boardOf("bob")));
        assertEquals(Street.STREET_1, hand.street());

        List<Card> pending = hand.boardOf("alice").pendingCards();
        List<Placement> placements = List.of(
                new Placement(pending.get(0), Row.MIDDLE),
                new Placement(pending.get(1), Row.MIDDLE));
        TurnMove missingDiscard = new TurnMove("alice", placements, null);
        assertThrows(IllegalMoveException.class, () -> hand.applyMove(missingDiscard));
    }

    @Test
    void fantasylandPlayerActsImmediatelyWithoutWaitingForTurn() {
        PineappleHand hand = PineappleHand.deal(List.of("alice", "bob"), Set.of("bob"), new Random(10));
        assertEquals(13, hand.boardOf("bob").pendingCards().size());
        assertEquals("alice", hand.currentActor(), "fantasyland players are outside the normal turn order");

        hand.applyMove(planMove(hand.boardOf("bob")));
        assertTrue(hand.boardOf("bob").board().isComplete());
        assertFalse(hand.isComplete(), "alice still needs to play out her normal streets");

        TurnMove secondFantasylandMove = new TurnMove("bob", List.of(), null);
        assertThrows(IllegalMoveException.class, () -> hand.applyMove(secondFantasylandMove));
    }

    @Test
    void playingAFullTwoPlayerHandReachesCompletion() {
        PineappleHand hand = PineappleHand.deal(List.of("alice", "bob"), Set.of(), new Random(11));
        playFullHand(hand, List.of("alice", "bob"));
        assertTrue(hand.isComplete());
        assertTrue(hand.boardOf("alice").board().isComplete());
        assertTrue(hand.boardOf("bob").board().isComplete());
        assertNull(hand.currentActor());
    }

    @Test
    void restoringAHandPreservesStateAndAllowsPlayToContinue() {
        PineappleHand original = PineappleHand.deal(List.of("alice", "bob"), Set.of(), new Random(14));
        original.applyMove(planMove(original.boardOf("alice")));

        Map<String, PlayerBoard> restoredBoards = new java.util.LinkedHashMap<>();
        for (String playerId : original.seatOrder()) {
            PlayerBoard source = original.boardOf(playerId);
            poker.ofc.domain.board.Board restoredBoard = poker.ofc.domain.board.Board.restore(
                    source.board().cards(Row.TOP), source.board().cards(Row.MIDDLE), source.board().cards(Row.BOTTOM));
            restoredBoards.put(playerId, new PlayerBoard(playerId, source.isFantasyland(), restoredBoard, source.pendingCards()));
        }

        PineappleHand restored = PineappleHand.restore(original.seatOrder(), original.activeOrder(), restoredBoards,
                original.remainingDeckCards(), original.street(), original.turnPointer());

        assertEquals(original.street(), restored.street());
        assertEquals(original.currentActor(), restored.currentActor());
        assertEquals(original.boardOf("bob").pendingCards(), restored.boardOf("bob").pendingCards());

        // Play continues correctly from the restored state.
        restored.applyMove(planMove(restored.boardOf("bob")));
        assertEquals(Street.STREET_1, restored.street());
    }

    @Test
    void playingAFullThreePlayerHandReachesCompletion() {
        PineappleHand hand = PineappleHand.deal(List.of("alice", "bob", "cleo"), Set.of(), new Random(12));
        playFullHand(hand, List.of("alice", "bob", "cleo"));
        assertTrue(hand.isComplete());
    }

    private static List<Card> allCards() {
        List<Card> all = new ArrayList<>();
        for (poker.ofc.domain.card.Suit suit : poker.ofc.domain.card.Suit.values()) {
            for (poker.ofc.domain.card.Rank rank : poker.ofc.domain.card.Rank.values()) {
                all.add(new Card(rank, suit));
            }
        }
        return all;
    }
}
