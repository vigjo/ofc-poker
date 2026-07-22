package poker.ofc.room.persistence;

import poker.ofc.domain.game.HandResult;
import poker.ofc.room.RoomStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The full, trusted server-side state of a room at a point in time. This is
 * what gets serialized into DynamoDB; it intentionally includes information
 * (other players' pending cards, the remaining deck) that must never be sent
 * to clients as-is. {@code poker.ofc.room.api} is responsible for building a
 * player-filtered view from this before it goes over the wire.
 */
public record RoomSnapshot(String roomId, String inviteCode, long version, RoomStatus status,
                            List<SeatSnapshot> seats, HandSnapshot hand, HandResult lastHandResult,
                            Set<String> fantasylandNextHand, Map<String, Integer> runningTotals,
                            Instant createdAt, Instant lastActivityAt) {
}
