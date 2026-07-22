package poker.ofc.domain.board;

import org.junit.jupiter.api.Test;
import poker.ofc.domain.card.Card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardTest {

    private static void fill(Board board, Row row, String... codes) {
        for (String code : codes) {
            board.place(row, Card.fromCode(code));
        }
    }

    @Test
    void validBoardIsNotFouled() {
        Board board = new Board();
        fill(board, Row.TOP, "2h", "3d", "4c");
        fill(board, Row.MIDDLE, "5h", "5d", "5c", "9s", "2c");
        fill(board, Row.BOTTOM, "Th", "Jh", "Qh", "Kh", "Ah");
        assertTrue(board.isComplete());
        assertFalse(board.isFouled());
    }

    @Test
    void topBeatingMiddleFouls() {
        Board board = new Board();
        fill(board, Row.TOP, "9h", "9d", "9c");
        fill(board, Row.MIDDLE, "2h", "5d", "7c", "8s", "Kc");
        fill(board, Row.BOTTOM, "Th", "Jh", "Qh", "Kh", "Ah");
        assertTrue(board.isFouled());
        assertEquals(0, board.royaltyTotal(), "fouled boards forfeit all royalties");
    }

    @Test
    void middleBeatingBottomFouls() {
        Board board = new Board();
        fill(board, Row.TOP, "2h", "3d", "4c");
        fill(board, Row.MIDDLE, "9h", "9d", "9c", "9s", "2c");
        fill(board, Row.BOTTOM, "5h", "6d", "7c", "8s", "Kc");
        assertTrue(board.isFouled());
    }

    @Test
    void placingIntoFullRowThrows() {
        Board board = new Board();
        fill(board, Row.TOP, "2h", "3d", "4c");
        assertThrows(IllegalStateException.class, () -> board.place(Row.TOP, Card.fromCode("5h")));
    }

    @Test
    void royaltyTotalSumsAllThreeRows() {
        Board board = new Board();
        fill(board, Row.TOP, "6h", "6d", "2c"); // pair of sixes = 1
        fill(board, Row.MIDDLE, "9h", "9d", "9c", "2s", "3c"); // trips = 2
        fill(board, Row.BOTTOM, "Th", "Jh", "Qh", "Kh", "Ah"); // royal flush = 25
        assertEquals(28, board.royaltyTotal());
    }
}
