# Customer Service

## Overview
Customer Service manages customer data including profiles and addresses.

## Port
8081

## Prerequisites
Infrastructure must be running. See `../infrastructure-service/README.md` for setup.

## Run the Service
```bash
mvn spring-boot:run
```

## Health Check
```bash
curl http://localhost:8081/actuator/health
```

## Connection
- Connects to Couchbase at: `couchbase://localhost` (bucket: customer-data)
- Connects to Kafka at: `localhost:9092`
