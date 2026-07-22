package poker.ofc.room.persistence;

import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;

/**
 * Reports the task as not-ready if it cannot reach the rooms table, so the
 * ALB stops routing traffic to it (and, on ECS, so a task that boots but
 * loses DynamoDB connectivity gets cycled) instead of accepting requests it
 * cannot actually serve.
 */
@Readiness
public class DynamoDbHealthCheck implements HealthCheck {

    @Inject
    DynamoDbClient dynamoDbClient;

    private final String tableName;

    @Inject
    public DynamoDbHealthCheck(@ConfigProperty(name = "ofc.dynamodb.rooms-table") String roomsTable) {
        this.tableName = roomsTable;
    }

    @Override
    public HealthCheckResponse call() {
        try {
            dynamoDbClient.describeTable(DescribeTableRequest.builder().tableName(tableName).build());
            return HealthCheckResponse.up("dynamodb-rooms-table");
        } catch (Exception e) {
            return HealthCheckResponse.down("dynamodb-rooms-table");
        }
    }
}
