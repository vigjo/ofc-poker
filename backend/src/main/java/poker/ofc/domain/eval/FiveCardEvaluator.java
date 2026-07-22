package poker.ofc.domain.eval;

import poker.ofc.domain.card.Card;
import poker.ofc.domain.card.Rank;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Evaluates the standard poker strength of a 5-card hand (the middle and
 * bottom OFC rows). Stateless and safe to reuse/share.
 */
public final class FiveCardEvaluator {

    private FiveCardEvaluator() {
    }

    public static HandRank evaluate(List<Card> cards) {
        if (cards.size() != 5) {
            throw new IllegalArgumentException("Five card evaluator requires exactly 5 cards, got " + cards.size());
        }

        Map<Integer, Integer> rankCounts = new TreeMap<>(Comparator.reverseOrder());
        for (Card card : cards) {
            rankCounts.merge(card.rank().value(), 1, Integer::sum);
        }

        boolean isFlush = cards.stream().map(Card::suit).distinct().count() == 1;
        StraightResult straight = detectStraight(cards);

        if (isFlush && straight.isStraight()) {
            if (!straight.wheel() && straight.highCard() == Rank.ACE.value()) {
                return new HandRank(HandCategory.ROYAL_FLUSH, List.of(straight.highCard()));
            }
            return new HandRank(HandCategory.STRAIGHT_FLUSH, List.of(straight.highCard()));
        }

        List<Map.Entry<Integer, Integer>> byCountThenRank = new ArrayList<>(rankCounts.entrySet());
        byCountThenRank.sort((a, b) -> {
            int countCompare = Integer.compare(b.getValue(), a.getValue());
            return countCompare != 0 ? countCompare : Integer.compare(b.getKey(), a.getKey());
        });

        List<Integer> counts = byCountThenRank.stream().map(Map.Entry::getValue).toList();

        if (counts.equals(List.of(4, 1))) {
            return new HandRank(HandCategory.QUADS, tiebreakers(byCountThenRank));
        }
        if (counts.equals(List.of(3, 2))) {
            return new HandRank(HandCategory.FULL_HOUSE, tiebreakers(byCountThenRank));
        }
        if (isFlush) {
            return new HandRank(HandCategory.FLUSH, descendingRanks(cards));
        }
        if (straight.isStraight()) {
            return new HandRank(HandCategory.STRAIGHT, List.of(straight.highCard()));
        }
        if (counts.equals(List.of(3, 1, 1))) {
            return new HandRank(HandCategory.TRIPS, tiebreakers(byCountThenRank));
        }
        if (counts.equals(List.of(2, 2, 1))) {
            return new HandRank(HandCategory.TWO_PAIR, tiebreakers(byCountThenRank));
        }
        if (counts.equals(List.of(2, 1, 1, 1))) {
            return new HandRank(HandCategory.PAIR, tiebreakers(byCountThenRank));
        }
        return new HandRank(HandCategory.HIGH_CARD, descendingRanks(cards));
    }

    private static List<Integer> tiebreakers(List<Map.Entry<Integer, Integer>> byCountThenRank) {
        return byCountThenRank.stream().map(Map.Entry::getKey).toList();
    }

    private static List<Integer> descendingRanks(List<Card> cards) {
        return cards.stream()
                .map(card -> card.rank().value())
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    private static StraightResult detectStraight(List<Card> cards) {
        List<Integer> distinctRanks = cards.stream()
                .map(card -> card.rank().value())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
        if (distinctRanks.size() != 5) {
            return StraightResult.none();
        }

        boolean isWheel = distinctRanks.equals(List.of(14, 5, 4, 3, 2));
        if (isWheel) {
            return new StraightResult(true, true, 5);
        }

        for (int i = 0; i < 4; i++) {
            if (distinctRanks.get(i) - distinctRanks.get(i + 1) != 1) {
                return StraightResult.none();
            }
        }
        return new StraightResult(true, false, distinctRanks.get(0));
    }

    private record StraightResult(boolean isStraight, boolean wheel, int highCard) {
        static StraightResult none() {
            return new StraightResult(false, false, 0);
        }
    }
}
