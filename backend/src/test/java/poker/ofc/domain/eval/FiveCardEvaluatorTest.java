package poker.ofc.domain.eval;

import org.junit.jupiter.api.Test;
import poker.ofc.domain.card.Card;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FiveCardEvaluatorTest {

    private static List<Card> cards(String... codes) {
        return List.of(codes).stream().map(Card::fromCode).toList();
    }

    @Test
    void detectsHighCard() {
        HandRank rank = FiveCardEvaluator.evaluate(cards("2h", "5d", "9c", "Js", "Ah"));
        assertEquals(HandCategory.HIGH_CARD, rank.category());
    }

    @Test
    void detectsPair() {
        HandRank rank = FiveCardEvaluator.evaluate(cards("2h", "2d", "9c", "Js", "Ah"));
        assertEquals(HandCategory.PAIR, rank.category());
        assertEquals(2, rank.tiebreakers().get(0));
    }

    @Test
    void detectsTwoPair() {
        HandRank rank = FiveCardEvaluator.evaluate(cards("2h", "2d", "9c", "9s", "Ah"));
        assertEquals(HandCategory.TWO_PAIR, rank.category());
        assertEquals(List.of(9, 2, 14), rank.tiebreakers());
    }

    @Test
    void detectsTrips() {
        HandRank rank = FiveCardEvaluator.evaluate(cards("2h", "2d", "2c", "9s", "Ah"));
        assertEquals(HandCategory.TRIPS, rank.category());
    }

    @Test
    void detectsStraight() {
        HandRank rank = FiveCardEvaluator.evaluate(cards("5h", "6d", "7c", "8s", "9h"));
        assertEquals(HandCategory.STRAIGHT, rank.category());
        assertEquals(9, rank.tiebreakers().get(0));
    }

    @Test
    void detectsWheelStraightAsFiveHigh() {
        HandRank rank = FiveCardEvaluator.evaluate(cards("Ah", "2d", "3c", "4s", "5h"));
        assertEquals(HandCategory.STRAIGHT, rank.category());
        assertEquals(5, rank.tiebreakers().get(0));
    }

    @Test
    void detectsFlush() {
        HandRank rank = FiveCardEvaluator.evaluate(cards("2h", "6h", "9h", "Jh", "Kh"));
        assertEquals(HandCategory.FLUSH, rank.category());
    }

    @Test
    void detectsFullHouse() {
        HandRank rank = FiveCardEvaluator.evaluate(cards("2h", "2d", "2c", "9s", "9h"));
        assertEquals(HandCategory.FULL_HOUSE, rank.category());
        assertEquals(List.of(2, 9), rank.tiebreakers());
    }

    @Test
    void detectsQuads() {
        HandRank rank = FiveCardEvaluator.evaluate(cards("2h", "2d", "2c", "2s", "9h"));
        assertEquals(HandCategory.QUADS, rank.category());
    }

    @Test
    void detectsStraightFlush() {
        HandRank rank = FiveCardEvaluator.evaluate(cards("5h", "6h", "7h", "8h", "9h"));
        assertEquals(HandCategory.STRAIGHT_FLUSH, rank.category());
    }

    @Test
    void detectsWheelStraightFlushAsStraightFlushNotRoyal() {
        HandRank rank = FiveCardEvaluator.evaluate(cards("Ah", "2h", "3h", "4h", "5h"));
        assertEquals(HandCategory.STRAIGHT_FLUSH, rank.category());
        assertEquals(5, rank.tiebreakers().get(0));
    }

    @Test
    void detectsRoyalFlush() {
        HandRank rank = FiveCardEvaluator.evaluate(cards("Th", "Jh", "Qh", "Kh", "Ah"));
        assertEquals(HandCategory.ROYAL_FLUSH, rank.category());
    }

    @Test
    void royalFlushBeatsStraightFlush() {
        HandRank royal = FiveCardEvaluator.evaluate(cards("Th", "Jh", "Qh", "Kh", "Ah"));
        HandRank straightFlush = FiveCardEvaluator.evaluate(cards("5h", "6h", "7h", "8h", "9h"));
        assertTrue(royal.compareTo(straightFlush) > 0);
    }

    @Test
    void higherPairBeatsLowerPair() {
        HandRank aces = FiveCardEvaluator.evaluate(cards("Ah", "Ad", "9c", "Js", "2h"));
        HandRank twos = FiveCardEvaluator.evaluate(cards("2d", "2c", "9s", "Jh", "Kh"));
        assertTrue(aces.compareTo(twos) > 0);
    }

    @Test
    void rejectsWrongCardCount() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> FiveCardEvaluator.evaluate(cards("2h", "3h", "4h")));
    }
}
