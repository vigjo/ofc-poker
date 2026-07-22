package poker.ofc.room.api;

import poker.ofc.domain.game.HandResult;
import poker.ofc.domain.game.Street;
import poker.ofc.room.RoomStatus;

import java.util.List;
import java.util.Map;

public record RoomStateView(String roomId, String inviteCode, long version, RoomStatus status, String viewerPlayerId,
                             String currentActor, Street street, List<PlayerView> players, HandResult lastHandResult,
                             Map<String, Integer> runningTotals) {
}
