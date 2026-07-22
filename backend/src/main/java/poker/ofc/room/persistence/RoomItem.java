package poker.ofc.room.persistence;

import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;

/**
 * The flat DynamoDB row for a room. The full trusted state is stored as one
 * JSON blob ({@code stateJson}) rather than modeled as nested attributes;
 * this keeps the schema simple while every field that actually needs
 * conditional-write or query support - {@code version}, {@code status},
 * {@code ttlEpochSeconds} - stays a first-class attribute.
 *
 * <p>{@code version} mirrors {@code Room.version()} exactly (it is NOT the
 * Enhanced Client's own auto-incrementing optimistic-lock attribute); see
 * {@link RoomRepository#save} for the explicit conditional expression that
 * enforces one accepted write per game version.
 *
 * <p>The table schema is built explicitly with {@link StaticTableSchema}
 * rather than {@code TableSchema.fromBean}: the reflective bean-scanning
 * path generates classes at runtime against the thread context class
 * loader, which does not reliably see application classes across Quarkus's
 * layered test/dev class loaders. A hand-built schema is also marginally
 * faster to construct and has no such class-loading surprises in production.
 */
public class RoomItem {

    public static final TableSchema<RoomItem> TABLE_SCHEMA = StaticTableSchema.builder(RoomItem.class)
            .newItemSupplier(RoomItem::new)
            .addAttribute(String.class, a -> a.name("roomId")
                    .getter(RoomItem::getRoomId).setter(RoomItem::setRoomId)
                    .tags(StaticAttributeTags.primaryPartitionKey()))
            .addAttribute(Long.class, a -> a.name("version").getter(RoomItem::getVersion).setter(RoomItem::setVersion))
            .addAttribute(String.class, a -> a.name("status").getter(RoomItem::getStatus).setter(RoomItem::setStatus))
            .addAttribute(String.class, a -> a.name("stateJson").getter(RoomItem::getStateJson).setter(RoomItem::setStateJson))
            .addAttribute(String.class, a -> a.name("updatedAt").getter(RoomItem::getUpdatedAt).setter(RoomItem::setUpdatedAt))
            .addAttribute(Long.class, a -> a.name("ttlEpochSeconds")
                    .getter(RoomItem::getTtlEpochSeconds).setter(RoomItem::setTtlEpochSeconds))
            .build();

    private String roomId;
    private Long version;
    private String status;
    private String stateJson;
    private String updatedAt;
    private Long ttlEpochSeconds;

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStateJson() {
        return stateJson;
    }

    public void setStateJson(String stateJson) {
        this.stateJson = stateJson;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getTtlEpochSeconds() {
        return ttlEpochSeconds;
    }

    public void setTtlEpochSeconds(Long ttlEpochSeconds) {
        this.ttlEpochSeconds = ttlEpochSeconds;
    }
}
