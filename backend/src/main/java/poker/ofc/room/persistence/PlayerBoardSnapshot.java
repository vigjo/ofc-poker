package poker.ofc.room.persistence;

import poker.ofc.domain.card.Card;

import java.util.List;

public record PlayerBoardSnapshot(boolean fantasyland, List<Card> top, List<Card> middle, List<Card> bottom,
                                   List<Card> pendingCards) {
}
