# Runbook: DynamoDB restore drill

Production's rooms table has point-in-time recovery (PITR) enabled
(`infra/modules/dynamodb`, `enable_point_in_time_recovery = true`); staging
does not (cost trade-off - staging data is disposable). Run this drill
periodically (e.g. quarterly) so a real incident isn't the first time anyone
tries it.

## What PITR protects against

Accidental `PutItem` corruption, a bad deploy that writes malformed
`stateJson`, or an operator mistake - restoring to any point in the last 35
days. It does **not** protect against the table itself being deleted
(restore targets a *new* table name; the source table must still exist to
restore from its continuous backups) - that's a separate concern already
mitigated by `prevent_destroy`-style review discipline on `terraform plan`
output before any apply that touches the table resource.

## Drill procedure

1. Pick a timestamp a few minutes in the past.

   ```bash
   aws dynamodb restore-table-to-point-in-time \
     --source-table-name ofc-production-rooms \
     --target-table-name ofc-production-rooms-restore-drill \
     --restore-date-time "$(date -u -d '5 minutes ago' +%Y-%m-%dT%H:%M:%S)"
   ```

2. Wait for the restored table to become `ACTIVE`:

   ```bash
   aws dynamodb wait table-exists --table-name ofc-production-rooms-restore-drill
   ```

3. Spot-check a known room's item exists and its `stateJson` deserializes
   correctly:

   ```bash
   aws dynamodb get-item \
     --table-name ofc-production-rooms-restore-drill \
     --key '{"roomId": {"S": "<some-real-room-id>"}}'
   ```

4. Record the drill (date, duration, any issues) in your team's incident/ops
   log, then delete the restored table - it's not wired into the running
   application and exists only to prove the restore path works:

   ```bash
   aws dynamodb delete-table --table-name ofc-production-rooms-restore-drill
   ```

## If you ever need a *real* restore

Restore to a **new** table name (never directly overwrite the live table),
verify the data, then either:

- Update `OFC_DYNAMODB_TABLE_PREFIX` on the ECS task to point at the
  restored table and redeploy (fast, but leaves the old table around and
  diverges from what Terraform expects the table name to be - fix Terraform
  to match afterward), or
- Use the restored table's data to manually re-`PutItem` the specific
  room(s) you need back into the real table, then delete the restored copy.

Either way, players in an affected room will see a version/state mismatch
on their next move (the app is server-authoritative and will reject a stale
client) - tell them to refresh.
