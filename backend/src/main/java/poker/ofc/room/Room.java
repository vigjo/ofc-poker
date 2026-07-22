package poker.ofc.room;

import poker.ofc.domain.game.HandResult;
import poker.ofc.domain.game.IllegalMoveException;
import poker.ofc.domain.game.PineappleHand;
import poker.ofc.domain.game.PlayerResult;
import poker.ofc.domain.game.ScoringEngine;
import poker.ofc.domain.game.TurnMove;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A private table for 2-3 players across one or more hands. All mutating
 * operations are synchronized: a single room is expected to see modest,
 * bursty traffic from at most three people, so simple mutual exclusion is
 * far cheaper to reason about than fine-grained locking.
 */
public final class Room {

    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 3;
    private static final int MAX_RETAINED_EVENTS = 300;

    private final String roomId;
    private final String inviteCode;
    private final List<Seat> seats = new ArrayList<>();
    private final Map<String, Integer> runningTotals = new ConcurrentHashMap<>();
    private final Map<String, String> lastRequestIdByPlayer = new ConcurrentHashMap<>();
    private final Deque<RoomEvent> recentEvents = new ArrayDeque<>();
    private final Instant createdAt;

    private RoomStatus status = RoomStatus.WAITING;
    private PineappleHand currentHand;
    private HandResult lastHandResult;
    private Set<String> fantasylandNextHand = Set.of();
    private long version = 0;
    private Instant lastActivityAt;

    private Room(String roomId, String inviteCode, Instant createdAt) {
        this.roomId = roomId;
        this.inviteCode = inviteCode;
        this.createdAt = createdAt;
        this.lastActivityAt = createdAt;
    }

    public static Room create(String roomId, String inviteCode, String hostPlayerId, String hostDisplayName) {
        Room room = new Room(roomId, inviteCode, Instant.now());
        room.seats.add(new Seat(0, hostPlayerId, hostDisplayName, true));
        room.runningTotals.put(hostPlayerId, 0);
        // Version starts at 1 (not 0) so RoomRepository can treat "expected previous
        // version 0" as an unambiguous insert-only condition for every room's first save.
        room.touch();
        room.recordEvent(RoomEventType.PLAYER_JOINED, Map.of("playerId", hostPlayerId, "displayName", hostDisplayName));
        return room;
    }

    public synchronized Seat join(String playerId, String displayName) {
        if (status != RoomStatus.WAITING) {
            throw new RoomException(RoomErrorCode.ALREADY_STARTED, "Room " + roomId + " has already started");
        }
        if (seats.size() >= MAX_PLAYERS) {
            throw new RoomException(RoomErrorCode.ROOM_FULL, "Room " + roomId + " is full");
        }
        Seat seat = new Seat(seats.size(), playerId, displayName, false);
        seats.add(seat);
        runningTotals.put(playerId, 0);
        touch();
        recordEvent(RoomEventType.PLAYER_JOINED, Map.of("playerId", playerId, "displayName", displayName));
        return seat;
    }

    public synchronized void start(String requesterId) {
        requireHost(requesterId);
        if (status != RoomStatus.WAITING) {
            throw new RoomException(RoomErrorCode.ALREADY_STARTED, "Room " + roomId + " has already started");
        }
        if (seats.size() < MIN_PLAYERS) {
            throw new RoomException(RoomErrorCode.NOT_ENOUGH_PLAYERS, "Need at least " + MIN_PLAYERS + " players");
        }
        dealNewHand();
        status = RoomStatus.IN_PROGRESS;
        touch();
        recordEvent(RoomEventType.GAME_STARTED, Map.of("street", currentHand.street().name()));
    }

    public synchronized void startNextHand(String requesterId) {
        requireHost(requesterId);
        if (status != RoomStatus.HAND_COMPLETE) {
            throw new RoomException(RoomErrorCode.NOT_STARTED, "The current hand has not finished yet");
        }
        dealNewHand();
        status = RoomStatus.IN_PROGRESS;
        touch();
        recordEvent(RoomEventType.GAME_STARTED, Map.of("street", currentHand.street().name()));
    }

    private void dealNewHand() {
        List<String> seatOrder = seats.stream().map(Seat::playerId).toList();
        currentHand = PineappleHand.deal(seatOrder, fantasylandNextHand, new SecureRandom());
        lastHandResult = null;
    }

    /**
     * Applies a move. If {@code requestId} matches the last request accepted
     * from this player, the call is treated as a network retry of an
     * already-applied move and is silently ignored rather than re-validated
     * (which would fail, since the cards it referenced are no longer
     * pending) - this is the idempotency guarantee described in the plan.
     */
    public synchronized boolean applyMove(String playerId, String requestId, TurnMove move) {
        if (status != RoomStatus.IN_PROGRESS) {
            throw new RoomException(RoomErrorCode.NOT_STARTED, "Room " + roomId + " has no hand in progress");
        }
        requireSeated(playerId);
        if (requestId != null && requestId.equals(lastRequestIdByPlayer.get(playerId))) {
            return false;
        }
        try {
            currentHand.applyMove(move);
        } catch (IllegalMoveException e) {
            throw new RoomException(RoomErrorCode.ILLEGAL_MOVE, e.getMessage());
        }
        if (requestId != null) {
            lastRequestIdByPlayer.put(playerId, requestId);
        }
        touch();
        recordEvent(RoomEventType.MOVE_APPLIED, Map.of(
                "playerId", playerId,
                "placementCount", move.placements().size(),
                "discarded", move.discard() != null));

        if (currentHand.isComplete()) {
            completeHand();
        }
        return true;
    }

    private void completeHand() {
        lastHandResult = ScoringEngine.score(currentHand);
        for (PlayerResult playerResult : lastHandResult.players()) {
            runningTotals.merge(playerResult.playerId(), playerResult.total(), Integer::sum);
        }
        fantasylandNextHand = lastHandResult.players().stream()
                .filter(PlayerResult::fantasylandNextHand)
                .map(PlayerResult::playerId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        status = RoomStatus.HAND_COMPLETE;
        recordEvent(RoomEventType.HAND_COMPLETED, Map.of("results", lastHandResult.players()));
    }

    public synchronized void markDisconnected(String playerId) {
        requireSeated(playerId).markDisconnected();
        touch();
        recordEvent(RoomEventType.PLAYER_DISCONNECTED, Map.of("playerId", playerId));
    }

    public synchronized void markReconnected(String playerId) {
        requireSeated(playerId).markConnected();
        touch();
        recordEvent(RoomEventType.PLAYER_RECONNECTED, Map.of("playerId", playerId));
    }

    public synchronized boolean allDisconnected() {
        return seats.stream().noneMatch(Seat::isConnected);
    }

    private Seat requireSeated(String playerId) {
        return seats.stream()
                .filter(seat -> seat.playerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new RoomException(RoomErrorCode.UNKNOWN_PLAYER, playerId + " is not seated in this room"));
    }

    private void requireHost(String playerId) {
        Seat seat = requireSeated(playerId);
        if (!seat.isHost()) {
            throw new RoomException(RoomErrorCode.UNKNOWN_PLAYER, "Only the host can perform this action");
        }
    }

    private void touch() {
        version++;
        lastActivityAt = Instant.now();
    }

    private void recordEvent(RoomEventType type, Map<String, Object> payload) {
        recentEvents.addLast(new RoomEvent(version, type, payload, Instant.now()));
        while (recentEvents.size() > MAX_RETAINED_EVENTS) {
            recentEvents.removeFirst();
        }
    }

    public synchronized List<RoomEvent> eventsSince(long sinceVersion) {
        return recentEvents.stream().filter(e -> e.version() > sinceVersion).toList();
    }

    public synchronized boolean hasEventHistorySince(long sinceVersion) {
        return recentEvents.isEmpty() || recentEvents.peekFirst().version() - 1 <= sinceVersion;
    }

    // --- Plain accessors for the API/persistence layers (all read under the room's lock) ---

    public String roomId() {
        return roomId;
    }

    public String inviteCode() {
        return inviteCode;
    }

    public synchronized List<Seat> seats() {
        return List.copyOf(seats);
    }

    public synchronized RoomStatus status() {
        return status;
    }

    public synchronized PineappleHand currentHand() {
        return currentHand;
    }

    public synchronized HandResult lastHandResult() {
        return lastHandResult;
    }

    public synchronized Map<String, Integer> runningTotals() {
        return Map.copyOf(runningTotals);
    }

    public synchronized Set<String> fantasylandNextHand() {
        return Set.copyOf(fantasylandNextHand);
    }

    public synchronized long version() {
        return version;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public synchronized Instant lastActivityAt() {
        return lastActivityAt;
    }

    /** Restores a room's in-memory representation directly from persisted fields. */
    static Room restore(String roomId, String inviteCode, List<Seat> seats, RoomStatus status,
                         PineappleHand currentHand, HandResult lastHandResult, Set<String> fantasylandNextHand,
                         Map<String, Integer> runningTotals, long version, Instant createdAt, Instant lastActivityAt) {
        Room room = new Room(roomId, inviteCode, createdAt);
        room.seats.addAll(seats);
        room.status = status;
        room.currentHand = currentHand;
        room.lastHandResult = lastHandResult;
        room.fantasylandNextHand = fantasylandNextHand;
        room.runningTotals.putAll(runningTotals);
        room.version = version;
        room.lastActivityAt = lastActivityAt;
        return room;
    }
}
