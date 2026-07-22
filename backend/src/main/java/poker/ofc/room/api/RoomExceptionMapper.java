package poker.ofc.room.api;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import poker.ofc.room.RoomErrorCode;
import poker.ofc.room.RoomException;

@Provider
public class RoomExceptionMapper implements ExceptionMapper<RoomException> {

    @Override
    public Response toResponse(RoomException exception) {
        Response.Status status = switch (exception.code()) {
            case ROOM_NOT_FOUND, UNKNOWN_PLAYER -> Response.Status.NOT_FOUND;
            case INVALID_TOKEN -> Response.Status.UNAUTHORIZED;
            case ROOM_FULL, ALREADY_STARTED, NOT_ENOUGH_PLAYERS, NOT_STARTED, ILLEGAL_MOVE -> Response.Status.CONFLICT;
            case VERSION_CONFLICT -> Response.Status.CONFLICT;
        };
        return Response.status(status)
                .entity(new ErrorResponse(exception.code().name(), exception.getMessage()))
                .build();
    }
}
