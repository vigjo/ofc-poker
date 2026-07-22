package poker.ofc.domain.eval;

/**
 * Ordered from weakest to strongest. Ordinal order is used directly for
 * comparisons, so do not reorder these without updating {@link HandRank}.
 */
public enum HandCategory {
    HIGH_CARD,
    PAIR,
    TWO_PAIR,
    TRIPS,
    STRAIGHT,
    FLUSH,
    FULL_HOUSE,
    QUADS,
    STRAIGHT_FLUSH,
    ROYAL_FLUSH
}
