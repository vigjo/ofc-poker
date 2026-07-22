package poker.ofc.room;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import poker.ofc.domain.card.Card;
import poker.ofc.domain.game.Placement;
import poker.ofc.domain.game.TurnMove;
import poker.ofc.room.api.PlacementDto;
import poker.ofc.room.api.RoomStateView;
import poker.ofc.room.api.SeatCredentials;
import poker.ofc.room.auth.GuestTokenService;
import poker.ofc.room.persistence.RoomRepository;
import poker.ofc.room.ws.RoomBroadcaster;

import java.util.List;
import java.util.UUID;

/**
 * Application-facing entry point for everything room related. Every
 * mutating call re-persists the room's full snapshot with an optimistic
 * version check and pushes the fresh state to any open WebSocket
 * connections, so a crash between "applied in memory" and "saved" is the
 * only window where a very recent move could be lost - never corrupted or
 * duplicated.
 */
@ApplicationScoped
public class RoomService {

    private final RoomRegistry registry;
    private final RoomRepository repository;
    private final GuestTokenService tokenService;
    private final RoomViewMapper viewMapper;
    private final RoomBroadcaster broadcaster;

    @Inject
    public RoomService(RoomRegistry registry, RoomRepository repository, GuestTokenService tokenService,
                        RoomViewMapper viewMapper, RoomBroadcaster broadcaster) {
        this.registry = registry;
        this.repository = repository;
        this.tokenService = tokenService;
        this.viewMapper = viewMapper;
        this.broadcaster = broadcaster;
    }

    public SeatCredentials createRoom(String hostDisplayName) {
        String roomId = RoomIdGenerator.generate();
        String playerId = UUID.randomUUID().toString();
        Room room = Room.create(roomId, roomId, playerId, hostDisplayName);
        registry.register(room);
        persist(room, 0);
        String token = tokenService.issue(roomId, playerId, hostDisplayName);
        return new SeatCredentials(roomId, room.inviteCode(), playerId, hostDisplayName, token);
    }

    public SeatCredentials joinRoom(String roomId, String displayName) {
        Room room = registry.get(roomId);
        long before = room.version();
        String playerId = UUID.randomUUID().toString();
        room.join(playerId, displayName);
        persist(room, before);
        broadcaster.broadcastState(roomId);
        String token = tokenService.issue(roomId, playerId, displayName);
        return new SeatCredentials(roomId, room.inviteCode(), playerId, displayName, token);
    }

    public RoomStateView startGame(String roomId, String requesterId) {
        Room room = registry.get(roomId);
        long before = room.version();
        room.start(requesterId);
        persist(room, before);
        broadcaster.broadcastState(roomId);
        return viewMapper.viewFor(room, requesterId);
    }

    public RoomStateView startNextHand(String roomId, String requesterId) {
        Room room = registry.get(roomId);
        long before = room.version();
        room.startNextHand(requesterId);
        persist(room, before);
        broadcaster.broadcastState(roomId);
        return viewMapper.viewFor(room, requesterId);
    }

    public RoomStateView applyMove(String roomId, String playerId, String requestId, List<PlacementDto> placementDtos,
                                    Card discard) {
        Room room = registry.get(roomId);
        long before = room.version();
        List<Placement> placements = placementDtos.stream().map(p -> new Placement(p.card(), p.row())).toList();
        TurnMove move = new TurnMove(playerId, placements, discard);
        boolean applied = room.applyMove(playerId, requestId, move);
        if (applied) {
            persist(room, before);
            broadcaster.broadcastState(roomId);
        }
        return viewMapper.viewFor(room, playerId);
    }

    public RoomStateView getState(String roomId, String playerId) {
        Room room = registry.get(roomId);
        return viewMapper.viewFor(room, playerId);
    }

    public void markConnected(String roomId, String playerId) {
        Room room = registry.get(roomId);
        long before = room.version();
        room.markReconnected(playerId);
        persist(room, before);
    }

    public void markDisconnected(String roomId, String playerId) {
        Room room = registry.get(roomId);
        long before = room.version();
        room.markDisconnected(playerId);
        persist(room, before);
        if (room.allDisconnected()) {
            registry.remove(roomId);
        }
    }

    public List<RoomEvent> eventsSince(String roomId, long sinceVersion) {
        return registry.get(roomId).eventsSince(sinceVersion);
    }

    private void persist(Room room, long expectedPreviousVersion) {
        repository.save(RoomSnapshots.toSnapshot(room), expectedPreviousVersion);
    }
}
