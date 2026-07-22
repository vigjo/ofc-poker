package poker.ofc.domain.eval;

import poker.ofc.domain.card.Rank;

/**
 * Standard Pineapple OFC royalty tables. See {@code docs/rules.md} for the
 * published references these values are drawn from.
 */
public final class Royalties {

    private Royalties() {
    }

    /** Top row: pairs 66-AA score rank-5 (1..9); trips 222-AAA score rank+8 (10..22). */
    public static int topRow(HandRank rank) {
        return switch (rank.category()) {
            case PAIR -> {
                int pairRank = rank.tiebreakers().get(0);
                yield pairRank >= Rank.SIX.value() ? pairRank - 5 : 0;
            }
            case TRIPS -> rank.tiebreakers().get(0) + 8;
            default -> 0;
        };
    }

    public static int middleRow(HandRank rank) {
        return switch (rank.category()) {
            case TRIPS -> 2;
            case STRAIGHT -> 4;
            case FLUSH -> 8;
            case FULL_HOUSE -> 12;
            case QUADS -> 20;
            case STRAIGHT_FLUSH -> 30;
            case ROYAL_FLUSH -> 50;
            default -> 0;
        };
    }

    public static int bottomRow(HandRank rank) {
        return switch (rank.category()) {
            case STRAIGHT -> 2;
            case FLUSH -> 4;
            case FULL_HOUSE -> 6;
            case QUADS -> 10;
            case STRAIGHT_FLUSH -> 15;
            case ROYAL_FLUSH -> 25;
            default -> 0;
        };
    }
}
