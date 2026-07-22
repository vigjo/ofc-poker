package poker.ofc.domain.game;

/** The point swing between two players from one player's perspective. */
public record PairwiseResult(String opponentId, int rowPoints, int scoopBonus, int royaltyDifferential, int total) {

    static PairwiseResult of(String opponentId, int rowPoints, int scoopBonus, int royaltyDifferential) {
        return new PairwiseResult(opponentId, rowPoints, scoopBonus, royaltyDifferential,
                rowPoints + scoopBonus + royaltyDifferential);
    }
}
