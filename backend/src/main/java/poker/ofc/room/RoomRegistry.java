package poker.ofc.room;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import poker.ofc.room.persistence.RoomRepository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Holds the active, in-memory {@link Room} instances for this API process.
 * On a cache miss - most commonly right after a deploy/restart - the room is
 * hydrated from its last persisted DynamoDB snapshot instead of being
 * reported missing, which is how a single-task deployment survives restarts
 * without losing in-progress games.
 */
@ApplicationScoped
public class RoomRegistry {

    private final ConcurrentMap<String, Room> activeRooms = new ConcurrentHashMap<>();
    private final RoomRepository repository;

    @Inject
    public RoomRegistry(RoomRepository repository) {
        this.repository = repository;
    }

    public void register(Room room) {
        activeRooms.put(room.roomId(), room);
    }

    public Room get(String roomId) {
        Room cached = activeRooms.get(roomId);
        if (cached != null) {
            return cached;
        }
        Optional<Room> hydrated = repository.load(roomId).map(RoomSnapshots::fromSnapshot);
        if (hydrated.isEmpty()) {
            throw new RoomException(RoomErrorCode.ROOM_NOT_FOUND, "No room found with id " + roomId);
        }
        Room room = activeRooms.computeIfAbsent(roomId, id -> hydrated.get());
        return room;
    }

    public void remove(String roomId) {
        activeRooms.remove(roomId);
    }
}
