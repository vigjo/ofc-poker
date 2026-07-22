package poker.ofc.domain.game;

import poker.ofc.domain.card.Card;

import java.util.List;

/**
 * A player's submitted action for the street (or, for a fantasyland player,
 * their single whole-board action). {@code discard} is {@code null} for
 * streets with no discard and for fantasyland moves.
 */
public record TurnMove(String playerId, List<Placement> placements, Card discard) {

    public TurnMove {
        placements = List.copyOf(placements);
    }
}
