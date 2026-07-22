package poker.ofc.room.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import poker.ofc.domain.card.Card;

import java.util.List;

/**
 * {@code requestId} should be a client-generated UUID that stays the same
 * across retries of the same logical move, enabling idempotent handling on
 * the server (see {@code Room#applyMove}).
 */
public record MoveRequest(@NotBlank String requestId, @NotNull @Size(min = 1, max = 13) List<PlacementDto> placements,
                           Card discard) {
}
