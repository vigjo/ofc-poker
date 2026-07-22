package poker.ofc.room.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinRoomRequest(@NotBlank @Size(max = 24) String displayName) {
}
