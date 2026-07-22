package poker.ofc.room.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import poker.ofc.room.RoomException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.time.Instant;
import java.util.Optional;

import static poker.ofc.room.RoomErrorCode.VERSION_CONFLICT;

/**
 * Persists {@link RoomSnapshot}s as single-item, whole-state rows in
 * DynamoDB, using an explicit conditional expression (rather than the
 * Enhanced Client's built-in version extension) so {@code version} matches
 * {@code Room.version()} one-for-one and upstream reconnect/idempotency
 * logic can reason about it directly. Each successful {@link #save} accepts
 * exactly one writer per version, per the plan's "one accepted move per game
 * version" requirement.
 */
@ApplicationScoped
public class RoomRepository {

    private static final Logger LOG = Logger.getLogger(RoomRepository.class);

    private final DynamoDbTable<RoomItem> table;
    private final ObjectMapper objectMapper;

    @Inject
    public RoomRepository(DynamoDbEnhancedClient enhancedClient, DynamoDbClient plainClient, ObjectMapper objectMapper,
                           @ConfigProperty(name = "ofc.dynamodb.rooms-table") String tableName,
                           @ConfigProperty(name = "ofc.dynamodb.endpoint-override") Optional<String> endpointOverride) {
        this.table = enhancedClient.table(tableName, RoomItem.TABLE_SCHEMA);
        this.objectMapper = objectMapper;
        if (endpointOverride.isPresent() && !endpointOverride.get().isBlank()) {
            ensureLocalTableExists(plainClient, tableName);
        }
    }

    public void save(RoomSnapshot snapshot, long expectedPreviousVersion) {
        RoomItem item = new RoomItem();
        item.setRoomId(snapshot.roomId());
        item.setVersion(snapshot.version());
        item.setStatus(snapshot.status().name());
        item.setUpdatedAt(Instant.now().toString());
        item.setTtlEpochSeconds(snapshot.lastActivityAt().plusSeconds(24 * 3600).getEpochSecond());
        try {
            item.setStateJson(objectMapper.writeValueAsString(snapshot));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize room snapshot for " + snapshot.roomId(), e);
        }

        try {
            table.putItem(PutItemEnhancedRequest.builder(RoomItem.class)
                    .item(item)
                    .conditionExpression(versionCondition(expectedPreviousVersion))
                    .build());
        } catch (ConditionalCheckFailedException e) {
            throw new RoomException(VERSION_CONFLICT,
                    "Room " + snapshot.roomId() + " was updated concurrently; expected version " + expectedPreviousVersion);
        }
    }

    private Expression versionCondition(long expectedPreviousVersion) {
        if (expectedPreviousVersion == 0) {
            return Expression.builder().expression("attribute_not_exists(roomId)").build();
        }
        return Expression.builder()
                .expression("version = :expectedVersion")
                .putExpressionValue(":expectedVersion", AttributeValue.builder().n(Long.toString(expectedPreviousVersion)).build())
                .build();
    }

    public Optional<RoomSnapshot> load(String roomId) {
        RoomItem item = table.getItem(Key.builder().partitionValue(roomId).build());
        if (item == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(item.getStateJson(), RoomSnapshot.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize room snapshot for " + roomId, e);
        }
    }

    private void ensureLocalTableExists(DynamoDbClient client, String tableName) {
        try {
            client.createTable(CreateTableRequest.builder()
                    .tableName(tableName)
                    .keySchema(KeySchemaElement.builder().attributeName("roomId").keyType(KeyType.HASH).build())
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("roomId").attributeType(ScalarAttributeType.S).build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
            LOG.infof("Created local DynamoDB table %s", tableName);
        } catch (ResourceInUseException alreadyExists) {
            // Table already exists from a previous run; nothing to do.
        }
    }
}
