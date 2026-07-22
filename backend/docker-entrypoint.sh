#!/bin/sh
# Fargate has no native "mount a Secrets Manager secret as a file" primitive,
# so the task definition injects the JWT keypair as environment variables
# (via ECS `secrets`, backed by Secrets Manager) and this entrypoint writes
# them out to the file paths SmallRye JWT is configured to read
# (OFC_JWT_PRIVATE_KEY_PATH / OFC_JWT_PUBLIC_KEY_PATH). Local dev and CI never
# set these env vars, so this is a no-op there - the classpath PEMs checked
# into src/main/resources are used instead.
set -eu

if [ -n "${OFC_JWT_PRIVATE_KEY_PEM:-}" ]; then
  key_path="${OFC_JWT_PRIVATE_KEY_PATH:-/app/secrets/privateKey.pem}"
  mkdir -p "$(dirname "$key_path")"
  printf '%s' "$OFC_JWT_PRIVATE_KEY_PEM" >"$key_path"
  chmod 600 "$key_path"
fi

if [ -n "${OFC_JWT_PUBLIC_KEY_PEM:-}" ]; then
  key_path="${OFC_JWT_PUBLIC_KEY_PATH:-/app/secrets/publicKey.pem}"
  mkdir -p "$(dirname "$key_path")"
  printf '%s' "$OFC_JWT_PUBLIC_KEY_PEM" >"$key_path"
  chmod 600 "$key_path"
fi

exec java $JAVA_OPTS -jar quarkus-run.jar
