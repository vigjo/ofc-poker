package poker.ofc.domain.game;

import poker.ofc.domain.board.Board;
import poker.ofc.domain.board.Row;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Scores a set of completed boards using the standard head-to-head Pineapple
 * OFC "1-6" convention: each pair of players compares rows independently
 * (+1/-1/0 per row), a 3-0 sweep of the three rows adds a further +/-3 scoop
 * bonus, and each player's royalty total (already zero if fouled) is added
 * as a differential against every opponent. A player who fouls automatically
 * loses all three rows (and is scooped) against any opponent who did not
 * foul; two fouled players push against each other. See docs/rules.md.
 */
public final class ScoringEngine {

    private ScoringEngine() {
    }

    public static HandResult score(List<String> playerIds, Map<String, Board> boards) {
        for (String playerId : playerIds) {
            if (!boards.get(playerId).isComplete()) {
                throw new IllegalStateException("Cannot score an incomplete board for " + playerId);
            }
        }

        List<PlayerResult> results = new ArrayList<>();
        for (String playerId : playerIds) {
            Board self = boards.get(playerId);
            List<PairwiseResult> vsOpponents = new ArrayList<>();
            int total = 0;

            for (String opponentId : playerIds) {
                if (opponentId.equals(playerId)) {
                    continue;
                }
                PairwiseResult result = scorePair(self, boards.get(opponentId), opponentId);
                vsOpponents.add(result);
                total += result.total();
            }

            boolean fantasylandNext = self.qualifiesForFantasyland() || self.qualifiesToStayInFantasyland();
            results.add(new PlayerResult(playerId, self.isFouled(), self.royaltyTotal(), total, vsOpponents, fantasylandNext));
        }
        return new HandResult(results);
    }

    /** Convenience overload for scoring a fully played {@link PineappleHand} directly. */
    public static HandResult score(PineappleHand hand) {
        if (!hand.isComplete()) {
            throw new IllegalStateException("Cannot score an incomplete hand");
        }
        Map<String, Board> boards = new java.util.LinkedHashMap<>();
        for (String playerId : hand.seatOrder()) {
            boards.put(playerId, hand.boardOf(playerId).board());
        }
        return score(hand.seatOrder(), boards);
    }

    private static PairwiseResult scorePair(Board self, Board opponent, String opponentId) {
        boolean selfFouled = self.isFouled();
        boolean opponentFouled = opponent.isFouled();
        int royaltyDifferential = self.royaltyTotal() - opponent.royaltyTotal();

        if (selfFouled && opponentFouled) {
            return PairwiseResult.of(opponentId, 0, 0, 0);
        }
        if (selfFouled) {
            return PairwiseResult.of(opponentId, -3, -3, royaltyDifferential);
        }
        if (opponentFouled) {
            return PairwiseResult.of(opponentId, 3, 3, royaltyDifferential);
        }

        int rowPoints = 0;
        for (Row row : Row.values()) {
            rowPoints += Integer.signum(self.handRank(row).compareTo(opponent.handRank(row)));
        }
        int scoopBonus = rowPoints == 3 ? 3 : rowPoints == -3 ? -3 : 0;
        return PairwiseResult.of(opponentId, rowPoints, scoopBonus, royaltyDifferential);
    }
}
