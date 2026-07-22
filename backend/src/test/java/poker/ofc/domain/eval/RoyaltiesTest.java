package poker.ofc.domain.eval;

import org.junit.jupiter.api.Test;
import poker.ofc.domain.card.Card;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoyaltiesTest {

    private static List<Card> cards(String... codes) {
        return List.of(codes).stream().map(Card::fromCode).toList();
    }

    @Test
    void topRowPairBelowSixScoresZero() {
        HandRank rank = ThreeCardEvaluator.evaluate(cards("5h", "5d", "2c"));
        assertEquals(0, Royalties.topRow(rank));
    }

    @Test
    void topRowPairSixesScoresOne() {
        HandRank rank = ThreeCardEvaluator.evaluate(cards("6h", "6d", "2c"));
        assertEquals(1, Royalties.topRow(rank));
    }

    @Test
    void topRowPairAcesScoresNine() {
        HandRank rank = ThreeCardEvaluator.evaluate(cards("Ah", "Ad", "2c"));
        assertEquals(9, Royalties.topRow(rank));
    }

    @Test
    void topRowTripsDeucesScoresTen() {
        HandRank rank = ThreeCardEvaluator.evaluate(cards("2h", "2d", "2c"));
        assertEquals(10, Royalties.topRow(rank));
    }

    @Test
    void topRowTripsAcesScoresTwentyTwo() {
        HandRank rank = ThreeCardEvaluator.evaluate(cards("Ah", "Ad", "Ac"));
        assertEquals(22, Royalties.topRow(rank));
    }

    @Test
    void topRowTripsJacksScoresNineteen() {
        HandRank rank = ThreeCardEvaluator.evaluate(cards("Jh", "Jd", "Jc"));
        assertEquals(19, Royalties.topRow(rank), "the legacy prototype's enum incorrectly skipped this value");
    }

    @Test
    void middleRowRoyaltyTable() {
        assertEquals(2, Royalties.middleRow(new HandRank(HandCategory.TRIPS, List.of(2))));
        assertEquals(4, Royalties.middleRow(new HandRank(HandCategory.STRAIGHT, List.of(9))));
        assertEquals(8, Royalties.middleRow(new HandRank(HandCategory.FLUSH, List.of())));
        assertEquals(12, Royalties.middleRow(new HandRank(HandCategory.FULL_HOUSE, List.of())));
        assertEquals(20, Royalties.middleRow(new HandRank(HandCategory.QUADS, List.of())));
        assertEquals(30, Royalties.middleRow(new HandRank(HandCategory.STRAIGHT_FLUSH, List.of())));
        assertEquals(50, Royalties.middleRow(new HandRank(HandCategory.ROYAL_FLUSH, List.of())));
        assertEquals(0, Royalties.middleRow(new HandRank(HandCategory.PAIR, List.of())));
    }

    @Test
    void bottomRowRoyaltyTable() {
        assertEquals(2, Royalties.bottomRow(new HandRank(HandCategory.STRAIGHT, List.of())));
        assertEquals(4, Royalties.bottomRow(new HandRank(HandCategory.FLUSH, List.of())));
        assertEquals(6, Royalties.bottomRow(new HandRank(HandCategory.FULL_HOUSE, List.of())));
        assertEquals(10, Royalties.bottomRow(new HandRank(HandCategory.QUADS, List.of())));
        assertEquals(15, Royalties.bottomRow(new HandRank(HandCategory.STRAIGHT_FLUSH, List.of())));
        assertEquals(25, Royalties.bottomRow(new HandRank(HandCategory.ROYAL_FLUSH, List.of())));
        assertEquals(0, Royalties.bottomRow(new HandRank(HandCategory.TRIPS, List.of())));
    }
}
