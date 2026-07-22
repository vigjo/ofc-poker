package poker.ofc.room.api;

import jakarta.validation.constraints.NotNull;
import poker.ofc.domain.board.Row;
import poker.ofc.domain.card.Card;

public record PlacementDto(@NotNull Card card, @NotNull Row row) {
}
