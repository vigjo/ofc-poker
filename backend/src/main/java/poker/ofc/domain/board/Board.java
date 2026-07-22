package poker.ofc.domain.board;

import poker.ofc.domain.card.Card;
import poker.ofc.domain.card.Rank;
import poker.ofc.domain.eval.FiveCardEvaluator;
import poker.ofc.domain.eval.HandCategory;
import poker.ofc.domain.eval.HandRank;
import poker.ofc.domain.eval.Royalties;
import poker.ofc.domain.eval.ThreeCardEvaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A single player's 3/5/5 board. All placements are open-face: once a card
 * is placed it is immediately visible to every player and can never be
 * moved or removed. This class only enforces row capacity; turn/street
 * legality is enforced by {@code poker.ofc.domain.game}.
 */
public final class Board {

    private final Map<Row, List<Card>> rows;

    public Board() {
        this.rows = new EnumMap<>(Row.class);
        for (Row row : Row.values()) {
            rows.put(row, new ArrayList<>(row.capacity()));
        }
    }

    /** Rebuilds a board from previously persisted row contents. */
    public static Board restore(List<Card> top, List<Card> middle, List<Card> bottom) {
        Board board = new Board();
        top.forEach(card -> board.place(Row.TOP, card));
        middle.forEach(card -> board.place(Row.MIDDLE, card));
        bottom.forEach(card -> board.place(Row.BOTTOM, card));
        return board;
    }

    public void place(Row row, Card card) {
        List<Card> cards = rows.get(row);
        if (cards.size() >= row.capacity()) {
            throw new IllegalStateException("Row " + row + " is already full");
        }
        cards.add(card);
    }

    public List<Card> cards(Row row) {
        return Collections.unmodifiableList(rows.get(row));
    }

    public boolean isRowFull(Row row) {
        return rows.get(row).size() == row.capacity();
    }

    public int cardCount() {
        return rows.values().stream().mapToInt(List::size).sum();
    }

    public boolean isComplete() {
        return cardCount() == 13;
    }

    public int remainingCapacity(Row row) {
        return row.capacity() - rows.get(row).size();
    }

    public HandRank handRank(Row row) {
        if (!isRowFull(row)) {
            throw new IllegalStateException("Row " + row + " is not yet full");
        }
        List<Card> cards = rows.get(row);
        return row == Row.TOP ? ThreeCardEvaluator.evaluate(cards) : FiveCardEvaluator.evaluate(cards);
    }

    /** Only meaningful once {@link #isComplete()} is true. */
    public boolean isFouled() {
        HandRank top = handRank(Row.TOP);
        HandRank middle = handRank(Row.MIDDLE);
        HandRank bottom = handRank(Row.BOTTOM);
        return top.compareTo(middle) > 0 || middle.compareTo(bottom) > 0;
    }

    /** Total royalty points for this board; 0 for a fouled board. */
    public int royaltyTotal() {
        if (!isComplete() || isFouled()) {
            return 0;
        }
        return Royalties.topRow(handRank(Row.TOP))
                + Royalties.middleRow(handRank(Row.MIDDLE))
                + Royalties.bottomRow(handRank(Row.BOTTOM));
    }

    /**
     * Whether this (complete, non-fouled) board qualifies to enter
     * fantasyland on the next hand: a top pair of queens or better, or top
     * trips.
     */
    public boolean qualifiesForFantasyland() {
        if (!isComplete() || isFouled()) {
            return false;
        }
        HandRank top = handRank(Row.TOP);
        if (top.category() == HandCategory.TRIPS) {
            return true;
        }
        return top.category() == HandCategory.PAIR && top.tiebreakers().get(0) >= Rank.QUEEN.value();
    }

    /**
     * Whether this (complete, non-fouled) board qualifies to stay in
     * fantasyland for the next hand: top trips, or a bottom row of a full
     * house or better.
     */
    public boolean qualifiesToStayInFantasyland() {
        if (!isComplete() || isFouled()) {
            return false;
        }
        boolean topTrips = handRank(Row.TOP).category() == HandCategory.TRIPS;
        boolean bottomStrong = handRank(Row.BOTTOM).category().compareTo(HandCategory.FULL_HOUSE) >= 0;
        return topTrips || bottomStrong;
    }
}
