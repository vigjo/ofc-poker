package poker.ofc.room;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import poker.ofc.domain.board.Row;
import poker.ofc.domain.card.Card;
import poker.ofc.room.api.PlacementDto;
import poker.ofc.room.api.RoomStateView;
import poker.ofc.room.api.SeatCredentials;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end coverage of the room REST API against a real (DynamoDB Local
 * backed) {@link RoomService}, exercising exactly the path production
 * traffic takes: create, join, start, play a full 2-player hand through
 * every street, and confirm idempotent move retries and stale-token
 * rejection behave as documented in the plan.
 *
 * <p>Requires DynamoDB Local running locally (see docker-compose.yml). Named
 * with the Failsafe {@code *IT} suffix so it is skipped by the fast
 * {@code mvn test} unit loop and only runs during {@code mvn verify
 * -DskipITs=false} (see .github/workflows/backend-ci.yml).
 */
@QuarkusTest
class RoomLifecycleIT {

    @Inject
    RoomRegistry registry;

    @Test
    void playsACompleteTwoPlayerHandToCompletion() {
        SeatCredentials host = createRoom("Alice");
        SeatCredentials guest = joinRoom(host.roomId(), "Bob");

        RoomStateView started = startGame(host.roomId(), host.seatToken());
        assertThat(started.status()).isEqualTo(RoomStatus.IN_PROGRESS);
        assertThat(started.players()).hasSize(2);

        RoomStateView state = getState(host.roomId(), host.seatToken());
        while (state.status() == RoomStatus.IN_PROGRESS) {
            String actor = state.currentActor();
            String actorToken = actor.equals(host.playerId()) ? host.seatToken() : guest.seatToken();
            RoomStateView actorState = getState(host.roomId(), actorToken);
            state = playTurn(host.roomId(), actor, actorToken, actorState);
        }

        assertThat(state.status()).isEqualTo(RoomStatus.HAND_COMPLETE);
        assertThat(state.lastHandResult()).isNotNull();
        assertThat(state.lastHandResult().players()).hasSize(2);
        assertThat(state.runningTotals()).containsKeys(host.playerId(), guest.playerId());

        for (var player : state.players()) {
            assertThat(player.top().size() + player.middle().size() + player.bottom().size()).isEqualTo(13);
            assertThat(player.pendingCount()).isZero();
        }
    }

    @Test
    void duplicateRequestIdIsIgnoredInsteadOfReapplied() {
        SeatCredentials host = createRoom("Carol");
        SeatCredentials guest = joinRoom(host.roomId(), "Dan");
        startGame(host.roomId(), host.seatToken());

        RoomStateView state = getState(host.roomId(), host.seatToken());
        String actor = state.currentActor();
        String actorToken = actor.equals(host.playerId()) ? host.seatToken() : guest.seatToken();
        RoomStateView actorState = getState(host.roomId(), actorToken);
        var actorView = actorState.players().stream().filter(p -> p.playerId().equals(actor)).findFirst().orElseThrow();
        List<Card> pending = actorView.pendingCards();
        String requestId = UUID.randomUUID().toString();
        List<PlacementDto> placements = List.of(
                new PlacementDto(pending.get(0), Row.TOP),
                new PlacementDto(pending.get(1), Row.MIDDLE),
                new PlacementDto(pending.get(2), Row.MIDDLE),
                new PlacementDto(pending.get(3), Row.BOTTOM),
                new PlacementDto(pending.get(4), Row.BOTTOM));

        RoomStateView afterFirst = submitMove(host.roomId(), actorToken, requestId, placements, null);
        RoomStateView afterRetry = submitMove(host.roomId(), actorToken, requestId, placements, null);

        assertThat(afterRetry.version()).isEqualTo(afterFirst.version());
        assertThat(afterRetry.currentActor()).isEqualTo(afterFirst.currentActor());
    }

    @Test
    void roomStateSurvivesAnApiProcessRestart() {
        SeatCredentials host = createRoom("Frank");
        SeatCredentials guest = joinRoom(host.roomId(), "Grace");
        RoomStateView beforeRestart = startGame(host.roomId(), host.seatToken());

        // Simulate this API task restarting: drop the in-memory Room, forcing
        // the next lookup to rehydrate from the last persisted DynamoDB
        // snapshot (see RoomRegistry#get).
        registry.remove(host.roomId());

        RoomStateView afterRestart = getState(host.roomId(), host.seatToken());
        assertThat(afterRestart.version()).isEqualTo(beforeRestart.version());
        assertThat(afterRestart.status()).isEqualTo(RoomStatus.IN_PROGRESS);
        assertThat(afterRestart.currentActor()).isEqualTo(beforeRestart.currentActor());
        assertThat(afterRestart.players()).hasSize(2);

        // The rehydrated room must still accept new moves, versioned from
        // where the snapshot left off.
        String actor = afterRestart.currentActor();
        String actorToken = actor.equals(host.playerId()) ? host.seatToken() : guest.seatToken();
        RoomStateView actorState = getState(host.roomId(), actorToken);
        RoomStateView afterMove = playTurn(host.roomId(), actor, actorToken, actorState);
        assertThat(afterMove.version()).isGreaterThan(afterRestart.version());
    }

    @Test
    void responsesCarryCorrelationIdsAndSecurityHeaders() {
        given()
                .contentType(ContentType.JSON)
                .header("X-Request-Id", "test-correlation-id")
                .body("{\"displayName\":\"Heidi\"}")
                .when().post("/api/rooms")
                .then().statusCode(200)
                .header("X-Request-Id", "test-correlation-id")
                .header("X-Content-Type-Options", "nosniff")
                .header("X-Frame-Options", "DENY")
                .header("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");

        given()
                .contentType(ContentType.JSON)
                .body("{\"displayName\":\"Ivan\"}")
                .when().post("/api/rooms")
                .then().statusCode(200)
                .header("X-Request-Id", org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString()));
    }

    @Test
    void requestsWithoutAValidSeatTokenAreRejected() {
        SeatCredentials host = createRoom("Eve");

        given()
                .header("Authorization", "Bearer not-a-real-token")
                .when().get("/api/rooms/{roomId}/state", host.roomId())
                .then().statusCode(401);

        given()
                .when().get("/api/rooms/{roomId}/state", host.roomId())
                .then().statusCode(401);
    }

    private RoomStateView playTurn(String roomId, String actor, String actorToken, RoomStateView state) {
        var actorView = state.players().stream().filter(p -> p.playerId().equals(actor)).findFirst().orElseThrow();
        List<Card> pending = actorView.pendingCards();
        List<PlacementDto> placements;
        Card discard = null;

        if (pending.size() == 5) {
            placements = List.of(
                    new PlacementDto(pending.get(0), Row.TOP),
                    new PlacementDto(pending.get(1), Row.MIDDLE),
                    new PlacementDto(pending.get(2), Row.MIDDLE),
                    new PlacementDto(pending.get(3), Row.BOTTOM),
                    new PlacementDto(pending.get(4), Row.BOTTOM));
        } else {
            Row[] rows = rowsForStreet(state.street().name());
            placements = List.of(new PlacementDto(pending.get(0), rows[0]), new PlacementDto(pending.get(1), rows[1]));
            discard = pending.get(2);
        }

        return submitMove(roomId, actorToken, UUID.randomUUID().toString(), placements, discard);
    }

    private Row[] rowsForStreet(String streetName) {
        return switch (streetName) {
            case "STREET_1", "STREET_2" -> new Row[] {Row.TOP, Row.MIDDLE};
            case "STREET_3" -> new Row[] {Row.MIDDLE, Row.BOTTOM};
            default -> new Row[] {Row.BOTTOM, Row.BOTTOM};
        };
    }

    private SeatCredentials createRoom(String displayName) {
        return given()
                .contentType(ContentType.JSON)
                .body("{\"displayName\":\"" + displayName + "\"}")
                .when().post("/api/rooms")
                .then().statusCode(200)
                .extract().as(SeatCredentials.class);
    }

    private SeatCredentials joinRoom(String roomId, String displayName) {
        return given()
                .contentType(ContentType.JSON)
                .body("{\"displayName\":\"" + displayName + "\"}")
                .when().post("/api/rooms/{roomId}/join", roomId)
                .then().statusCode(200)
                .extract().as(SeatCredentials.class);
    }

    private RoomStateView startGame(String roomId, String seatToken) {
        return given()
                .header("Authorization", "Bearer " + seatToken)
                .when().post("/api/rooms/{roomId}/start", roomId)
                .then().statusCode(200)
                .extract().as(RoomStateView.class);
    }

    private RoomStateView getState(String roomId, String seatToken) {
        return given()
                .header("Authorization", "Bearer " + seatToken)
                .when().get("/api/rooms/{roomId}/state", roomId)
                .then().statusCode(200)
                .extract().as(RoomStateView.class);
    }

    private RoomStateView submitMove(String roomId, String seatToken, String requestId, List<PlacementDto> placements,
                                      Card discard) {
        var body = new java.util.HashMap<String, Object>();
        body.put("requestId", requestId);
        body.put("placements", placements);
        body.put("discard", discard);
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + seatToken)
                .body(body)
                .when().post("/api/rooms/{roomId}/moves", roomId)
                .then().statusCode(200)
                .extract().as(RoomStateView.class);
    }
}
