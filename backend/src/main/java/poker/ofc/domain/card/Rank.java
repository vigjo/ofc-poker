package poker.ofc.domain.card;

/**
 * Card ranks ordered low to high. {@link #value} is the numeric rank used for
 * hand comparisons; the wheel straight (5-4-3-2-A) is handled separately by
 * the evaluators rather than by giving ACE a dual value here.
 */
public enum Rank {
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "T"),
    JACK(11, "J"),
    QUEEN(12, "Q"),
    KING(13, "K"),
    ACE(14, "A");

    private final int value;
    private final String symbol;

    Rank(int value, String symbol) {
        this.value = value;
        this.symbol = symbol;
    }

    public int value() {
        return value;
    }

    public String symbol() {
        return symbol;
    }

    public static Rank fromValue(int value) {
        for (Rank rank : values()) {
            if (rank.value == value) {
                return rank;
            }
        }
        throw new IllegalArgumentException("No rank with value " + value);
    }
}
