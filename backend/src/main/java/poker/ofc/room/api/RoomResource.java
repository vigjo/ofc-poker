package poker.ofc.room.api;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import poker.ofc.room.RoomErrorCode;
import poker.ofc.room.RoomException;
import poker.ofc.room.RoomService;
import poker.ofc.room.auth.GuestPrincipal;
import poker.ofc.room.auth.GuestTokenService;

/**
 * Private room lifecycle: create, join, start, read state, and submit moves.
 * Every endpoint except create/join requires a {@code Bearer} seat token
 * scoped to the room in the path.
 */
@Path("/api/rooms")
@Produces(MediaType.APPLICATION_JSON)
public class RoomResource {

    @Inject
    RoomService roomService;

    @Inject
    GuestTokenService tokenService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public SeatCredentials createRoom(@Valid CreateRoomRequest request) {
        return roomService.createRoom(request.displayName());
    }

    @POST
    @Path("/{roomId}/join")
    @Consumes(MediaType.APPLICATION_JSON)
    public SeatCredentials joinRoom(@PathParam("roomId") String roomId, @Valid JoinRoomRequest request) {
        return roomService.joinRoom(roomId, request.displayName());
    }

    @POST
    @Path("/{roomId}/start")
    public RoomStateView startGame(@PathParam("roomId") String roomId, @HeaderParam("Authorization") String authHeader) {
        GuestPrincipal principal = authenticate(roomId, authHeader);
        return roomService.startGame(roomId, principal.playerId());
    }

    @POST
    @Path("/{roomId}/next-hand")
    public RoomStateView startNextHand(@PathParam("roomId") String roomId, @HeaderParam("Authorization") String authHeader) {
        GuestPrincipal principal = authenticate(roomId, authHeader);
        return roomService.startNextHand(roomId, principal.playerId());
    }

    @GET
    @Path("/{roomId}/state")
    public RoomStateView getState(@PathParam("roomId") String roomId, @HeaderParam("Authorization") String authHeader) {
        GuestPrincipal principal = authenticate(roomId, authHeader);
        return roomService.getState(roomId, principal.playerId());
    }

    @POST
    @Path("/{roomId}/moves")
    @Consumes(MediaType.APPLICATION_JSON)
    public RoomStateView applyMove(@PathParam("roomId") String roomId, @HeaderParam("Authorization") String authHeader,
                                    @Valid MoveRequest request) {
        GuestPrincipal principal = authenticate(roomId, authHeader);
        return roomService.applyMove(roomId, principal.playerId(), request.requestId(), request.placements(), request.discard());
    }

    private GuestPrincipal authenticate(String roomId, String authHeader) {
        if (authHeader == null || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new RoomException(RoomErrorCode.INVALID_TOKEN, "Missing bearer seat token");
        }
        GuestPrincipal principal = tokenService.verify(authHeader.substring(7).trim());
        if (!principal.roomId().equals(roomId)) {
            throw new RoomException(RoomErrorCode.INVALID_TOKEN, "Seat token is not valid for this room");
        }
        return principal;
    }
}
