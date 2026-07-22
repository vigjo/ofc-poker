package poker.ofc.domain.game;

import org.junit.jupiter.api.Test;
import poker.ofc.domain.board.Board;
import poker.ofc.domain.board.Row;
import poker.ofc.domain.card.Card;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoringEngineTest {

    private static Board board(String top, String middle, String bottom) {
        Board board = new Board();
        place(board, Row.TOP, top);
        place(board, Row.MIDDLE, middle);
        place(board, Row.BOTTOM, bottom);
        return board;
    }

    private static void place(Board board, Row row, String codes) {
        for (String code : codes.split(" ")) {
            board.place(row, Card.fromCode(code));
        }
    }

    @Test
    void betterBoardWinsAllThreeRowsAndScoops() {
        // Pair of aces / trips nines / club flush: royalties 9 + 2 + 4 = 15.
        Board winner = board("Ah Ad 2h", "9h 9d 9c 3s 4c", "3c 5c 7c Jc Kc");
        assertFalse(winner.isFouled());
        assertEquals(15, winner.royaltyTotal());

        // High card / high card / pair of kings: no royalties, loses every row.
        Board loser = board("4d 5s 2d", "4h 5d 6s 8d Th", "Kh Kd 2s 3h 4s");
        assertFalse(loser.isFouled());
        assertEquals(0, loser.royaltyTotal());

        Map<String, Board> boards = new LinkedHashMap<>();
        boards.put("winner", winner);
        boards.put("loser", loser);

        HandResult result = ScoringEngine.score(List.of("winner", "loser"), boards);
        PlayerResult winnerResult = result.playerResult("winner");
        PlayerResult loserResult = result.playerResult("loser");

        assertEquals(3, winnerResult.vsOpponents().get(0).rowPoints());
        assertEquals(3, winnerResult.vsOpponents().get(0).scoopBonus());
        assertEquals(15, winnerResult.vsOpponents().get(0).royaltyDifferential());
        assertEquals(21, winnerResult.total());
        assertEquals(-21, loserResult.total());
        assertEquals(winnerResult.total(), -loserResult.total(), "two player scoring is zero-sum");
    }

    @Test
    void foulingPlayerAutomaticallyLosesAllRowsAndForfeitsRoyalties() {
        Board fouled = board("9h 9d 9c", "2h 3d 4c 5s 7h", "Th Jh Qh Kh 2d");
        assertTrue(fouled.isFouled(), "trips on top beats a high-card middle");

        Board clean = board("2s 3c 4d", "5h 6d 7s 8c Jd", "8d 8h 3s 4s 6s");
        assertFalse(clean.isFouled());
        assertEquals(0, clean.royaltyTotal());

        Map<String, Board> boards = new LinkedHashMap<>();
        boards.put("fouled", fouled);
        boards.put("clean", clean);

        HandResult result = ScoringEngine.score(List.of("fouled", "clean"), boards);
        PlayerResult fouledResult = result.playerResult("fouled");
        PlayerResult cleanResult = result.playerResult("clean");

        assertEquals(0, fouledResult.royalties(), "fouled boards forfeit all royalties");
        assertEquals(-6, fouledResult.total());
        assertEquals(6, cleanResult.total());
    }

    @Test
    void twoFouledPlayersPushAgainstEachOther() {
        Board fouledA = board("9h 9d 9c", "2h 3d 4c 5s 7h", "Th Jh Qh Kh 2d");
        Board fouledB = board("8h 8d 8c", "2s 3s 4s 5d 7d", "Tc Jc Qc Kc 2c");
        assertTrue(fouledA.isFouled());
        assertTrue(fouledB.isFouled());

        Map<String, Board> boards = new LinkedHashMap<>();
        boards.put("a", fouledA);
        boards.put("b", fouledB);

        HandResult result = ScoringEngine.score(List.of("a", "b"), boards);
        assertEquals(0, result.playerResult("a").total());
        assertEquals(0, result.playerResult("b").total());
    }

    @Test
    void royaltiesAreCountedAgainstEveryOpponentInAThreeWayHand() {
        // Pair of sixes / trips nines / diamond flush: royalties 1 + 2 + 4 = 7.
        Board royal = board("6h 6d 2c", "9h 9d 9c 3s 4c", "2d 5d 7d Jd Kd");
        assertFalse(royal.isFouled());
        assertEquals(7, royal.royaltyTotal());

        Board plainA = board("2h 3h 4h", "5h 7h 8h 2s 4s", "Ts Th Js Qs Ks");
        Board plainB = board("3d 4d 8d", "3c 5c 6c Td Qd", "Jc Jh Ac Kc 9s");
        assertFalse(plainA.isFouled());
        assertFalse(plainB.isFouled());
        assertEquals(0, plainA.royaltyTotal());
        assertEquals(0, plainB.royaltyTotal());

        Map<String, Board> boards = new LinkedHashMap<>();
        boards.put("royal", royal);
        boards.put("a", plainA);
        boards.put("b", plainB);

        HandResult result = ScoringEngine.score(List.of("royal", "a", "b"), boards);
        PlayerResult royalResult = result.playerResult("royal");

        assertEquals(2, royalResult.vsOpponents().size());
        assertEquals(7, royalResult.royalties());
        for (PairwiseResult pairwise : royalResult.vsOpponents()) {
            assertEquals(7, pairwise.royaltyDifferential(),
                    "the full royalty total is owed by every opponent independently, not split between them");
        }
    }
}
