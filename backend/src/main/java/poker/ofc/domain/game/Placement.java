package poker.ofc.domain.game;

import poker.ofc.domain.board.Row;
import poker.ofc.domain.card.Card;

public record Placement(Card card, Row row) {
}
