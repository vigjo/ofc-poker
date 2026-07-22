# Key schema, TTL attribute, and billing mode here must exactly match what
# RoomRepository/RoomItem expect (backend/src/main/java/poker/ofc/room/persistence).
# roomId-only partition key, no sort key, no GSIs - see docs/adr for why a
# single-item-per-room design is sufficient at this scale.
resource "aws_dynamodb_table" "rooms" {
  name         = "${var.table_prefix}rooms"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "roomId"

  attribute {
    name = "roomId"
    type = "S"
  }

  ttl {
    attribute_name = "ttlEpochSeconds"
    enabled        = true
  }

  point_in_time_recovery {
    enabled = var.enable_point_in_time_recovery
  }

  tags = { Name = "${var.table_prefix}rooms" }
}
