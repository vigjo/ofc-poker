package poker.ofc.domain.eval;

import poker.ofc.domain.card.Card;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Evaluates the top row (3 cards). Straights and flushes do not exist as
 * categories for a 3-card hand in Open Face Chinese poker; only high card,
 * pair, and trips apply.
 */
public final class ThreeCardEvaluator {

    private ThreeCardEvaluator() {
    }

    public static HandRank evaluate(List<Card> cards) {
        if (cards.size() != 3) {
            throw new IllegalArgumentException("Three card evaluator requires exactly 3 cards, got " + cards.size());
        }

        Map<Integer, Integer> rankCounts = new TreeMap<>(Comparator.reverseOrder());
        for (Card card : cards) {
            rankCounts.merge(card.rank().value(), 1, Integer::sum);
        }

        if (rankCounts.size() == 1) {
            int tripsRank = rankCounts.keySet().iterator().next();
            return new HandRank(HandCategory.TRIPS, List.of(tripsRank));
        }

        if (rankCounts.size() == 2) {
            int pairRank = rankCounts.entrySet().stream()
                    .filter(e -> e.getValue() == 2)
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElseThrow();
            int kicker = rankCounts.entrySet().stream()
                    .filter(e -> e.getValue() == 1)
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElseThrow();
            return new HandRank(HandCategory.PAIR, List.of(pairRank, kicker));
        }

        List<Integer> descending = cards.stream()
                .map(card -> card.rank().value())
                .sorted(Comparator.reverseOrder())
                .toList();
        return new HandRank(HandCategory.HIGH_CARD, descending);
    }
}
