package poker.ofc.domain.game;

import java.util.List;

public record PlayerResult(String playerId, boolean fouled, int royalties, int total,
                            List<PairwiseResult> vsOpponents, boolean fantasylandNextHand) {
}
