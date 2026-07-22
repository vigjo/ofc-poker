package poker.ofc.domain.eval;

import org.junit.jupiter.api.Test;
import poker.ofc.domain.card.Card;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreeCardEvaluatorTest {

    private static List<Card> cards(String... codes) {
        return List.of(codes).stream().map(Card::fromCode).toList();
    }

    @Test
    void detectsHighCard() {
        HandRank rank = ThreeCardEvaluator.evaluate(cards("2h", "5d", "9c"));
        assertEquals(HandCategory.HIGH_CARD, rank.category());
        assertEquals(List.of(9, 5, 2), rank.tiebreakers());
    }

    @Test
    void detectsPairWithKicker() {
        HandRank rank = ThreeCardEvaluator.evaluate(cards("9h", "9d", "2c"));
        assertEquals(HandCategory.PAIR, rank.category());
        assertEquals(List.of(9, 2), rank.tiebreakers());
    }

    @Test
    void detectsTrips() {
        HandRank rank = ThreeCardEvaluator.evaluate(cards("9h", "9d", "9c"));
        assertEquals(HandCategory.TRIPS, rank.category());
        assertEquals(List.of(9), rank.tiebreakers());
    }

    @Test
    void tripsBeatsPairBeatsHighCard() {
        HandRank trips = ThreeCardEvaluator.evaluate(cards("2h", "2d", "2c"));
        HandRank pair = ThreeCardEvaluator.evaluate(cards("Ah", "Ad", "Kc"));
        HandRank highCard = ThreeCardEvaluator.evaluate(cards("Ah", "Kd", "Qc"));
        assertTrue(trips.compareTo(pair) > 0, "even weak trips beat any pair");
        assertTrue(pair.compareTo(highCard) > 0);
    }

    @Test
    void rejectsWrongCardCount() {
        assertThrows(IllegalArgumentException.class, () -> ThreeCardEvaluator.evaluate(cards("2h", "3h")));
    }
}
