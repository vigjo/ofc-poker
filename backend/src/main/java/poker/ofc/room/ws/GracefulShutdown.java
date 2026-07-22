package poker.ofc.room.ws;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.OpenConnections;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Tells every connected client to reconnect elsewhere before the process
 * actually goes down, instead of letting them hit a hard socket reset. ECS
 * sends SIGTERM and then waits out the load balancer's deregistration delay
 * before killing the task, so this has time to run during a normal rolling
 * deploy or scale-in - clients see a clean "reconnecting" state rather than
 * an error.
 */
@ApplicationScoped
public class GracefulShutdown {

    private static final Logger LOG = Logger.getLogger(GracefulShutdown.class);
    private static final CloseReason GOING_AWAY = new CloseReason(1001, "Server shutting down, please reconnect");

    @Inject
    OpenConnections openConnections;

    void onShutdown(@Observes ShutdownEvent event) {
        var connections = openConnections.listAll();
        if (connections.isEmpty()) {
            return;
        }
        LOG.infof("Shutting down: closing %d open room connection(s) gracefully", connections.size());
        for (WebSocketConnection connection : connections) {
            try {
                connection.closeAndAwait(GOING_AWAY);
            } catch (Exception e) {
                LOG.debugf(e, "Ignoring error while closing connection %s during shutdown", connection.id());
            }
        }
    }
}
