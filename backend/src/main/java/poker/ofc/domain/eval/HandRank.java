package poker.ofc.domain.eval;

import java.util.List;

/**
 * The strength of a 3-card or 5-card hand. {@code tiebreakers} lists rank
 * values in descending order of significance (e.g. for two pair: high pair,
 * low pair, kicker) so two {@code HandRank}s of the same category can be
 * compared lexicographically.
 */
public record HandRank(HandCategory category, List<Integer> tiebreakers) implements Comparable<HandRank> {

    @Override
    public int compareTo(HandRank other) {
        int categoryCompare = this.category.compareTo(other.category);
        if (categoryCompare != 0) {
            return categoryCompare;
        }
        int len = Math.min(this.tiebreakers.size(), other.tiebreakers.size());
        for (int i = 0; i < len; i++) {
            int cmp = Integer.compare(this.tiebreakers.get(i), other.tiebreakers.get(i));
            if (cmp != 0) {
                return cmp;
            }
        }
        return Integer.compare(this.tiebreakers.size(), other.tiebreakers.size());
    }
}
