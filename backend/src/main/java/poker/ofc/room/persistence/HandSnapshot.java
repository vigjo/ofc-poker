package poker.ofc.room.persistence;

import poker.ofc.domain.card.Card;
import poker.ofc.domain.game.Street;

import java.util.List;
import java.util.Map;

public record HandSnapshot(List<String> seatOrder, List<String> activeOrder, Street street, int turnPointer,
                            List<Card> remainingDeck, Map<String, PlayerBoardSnapshot> boards) {
}
