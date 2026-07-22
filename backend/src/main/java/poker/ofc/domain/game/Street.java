package poker.ofc.domain.game;

/**
 * The dealing streets of a single Pineapple OFC hand. Every active (non
 * fantasyland) player is dealt {@link #dealCount()} cards, places
 * {@link #placeCount()} of them, and discards the rest.
 */
public enum Street {
    INITIAL(5, 5),
    STREET_1(3, 2),
    STREET_2(3, 2),
    STREET_3(3, 2),
    STREET_4(3, 2),
    COMPLETE(0, 0);

    private final int dealCount;
    private final int placeCount;

    Street(int dealCount, int placeCount) {
        this.dealCount = dealCount;
        this.placeCount = placeCount;
    }

    public int dealCount() {
        return dealCount;
    }

    public int placeCount() {
        return placeCount;
    }

    public int discardCount() {
        return dealCount - placeCount;
    }

    public Street next() {
        return switch (this) {
            case INITIAL -> STREET_1;
            case STREET_1 -> STREET_2;
            case STREET_2 -> STREET_3;
            case STREET_3 -> STREET_4;
            case STREET_4, COMPLETE -> COMPLETE;
        };
    }
}
