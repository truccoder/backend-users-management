#!/bin/sh
set -e

echo "Waiting for Kafka Connect to be ready..."
until curl -sf http://kafka-connect:8083/connectors; do
  echo "Kafka Connect is not ready yet - sleeping..."
  sleep 5
done

echo "Kafka Connect is ready. Registering Debezium PostgreSQL connector..."

curl -i -X POST http://kafka-connect:8083/connectors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "users-postgres-connector",
    "config": {
      "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
      "database.hostname": "users-postgresql",
      "database.port": "5432",
      "database.user": "postgres",
      "database.password": "postgres",
      "database.dbname": "users_localdev",
      "plugin.name": "pgoutput",
      "slot.name": "debezium_users_slot",
      "publication.name": "debezium_users_pub",
      "table.include.list": "users.t_users,users.t_friend_requests",
      "topic.prefix": "postgres",
      "key.converter": "org.apache.kafka.connect.json.JsonConverter",
      "key.converter.schemas.enable": "false",
      "value.converter": "org.apache.kafka.connect.json.JsonConverter",
      "value.converter.schemas.enable": "false",
      "decimal.handling.mode": "string",
      "time.precision.mode": "connect",
      "snapshot.mode": "initial"
    }
  }'

echo "Connector registration completed."