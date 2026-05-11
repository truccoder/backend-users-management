### Check all expected topics exist
```
docker exec -it users-kafka kafka-topics \
--bootstrap-server localhost:9092 \
--list
```

### Check connector is registered and RUNNING
```
curl -s http://localhost:8083/connectors/users-postgres-connector/status | jq .
```

### Consume from the CDC topics and watch live
#### Terminal 1 — listen to t_users CDC topic:
```
docker exec -it users-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic postgres.users.t_users \
  --from-beginning \
  --property print.timestamp=true \
  --property print.key=true \
  --property print.partition=true
```
#### Terminal 2 — insert a row to trigger a CDC event:
```
docker exec -it users-postgresql psql -U postgres -d users_localdev -c "
  INSERT INTO t_users (email, fullName, profilePictureUrl)
  VALUES ('test@example.com', 'testuser', 'https://example.com');
"
```

### Check the snapshot completed (from-beginning has historical rows)
```
docker exec -it users-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic postgres.users.t_users \
  --from-beginning \
  --max-messages 5 \
  --property print.key=true | jq .
```

### Verify the publication covers the right tables
```
docker exec -it users-postgresql psql -U postgres -d users_localdev -c "
  SELECT pubname, tablename
  FROM pg_publication_tables
  WHERE pubname = 'debezium_users_pub';
"
```