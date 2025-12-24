# Customer Service Architecture

## Layer Structure

### 1. Resource Layer (Entry Point)
- **Location**: `com.fooddelivery.resources`
- **Purpose**: Receives external HTTP calls, handles REST endpoints
- **Responsibility**: 
  - HTTP request/response handling
  - Delegates to service layer
  - Status code management
  - Input validation

### 2. Service Layer (Business Logic)
- **Location**: `com.fooddelivery.service`
- **Purpose**: Contains business logic
- **Responsibility**:
  - Business rules and validation
  - Calls repository in single line function calls
  - Calls external service APIs (via gateway layer) in single line calls
  - Timestamp management
  - Error handling

### 3. Persistence Layer (Database)
- **Location**: `com.fooddelivery.persistence`
- **Purpose**: Database operations
- **Components**:
  - **Models**: `com.fooddelivery.persistence.model` - Entity classes
  - **Repository**: `com.fooddelivery.persistence` - Repository interfaces

### 4. Gateway Layer (External Services)
- **Location**: `com.fooddelivery.gateway`
- **Purpose**: External service calls
- **Responsibility**: Calls to other microservices or external APIs

### 5. Config Layer (Configuration)
- **Location**: `com.fooddelivery.config`
- **Purpose**: Spring Boot configuration classes
- **Components**: Couchbase configuration, application bootstrap

## Request Flow

```
HTTP Request
    ↓
Resource Layer (Entry Point)
    ↓ (delegates to service)
Service Layer (Business Logic)
    ↓ (single line calls)
Repository Layer (Database) OR Gateway Layer (External APIs)
    ↓
Couchbase Database / External Services
```

## Key Principles

1. **Resource Layer is Entry Point**: All external calls come through Resource layer
2. **Service Layer has Business Logic**: All business rules in service layer
3. **Single Line Calls**: Service layer calls repository/gateway in single line function calls
4. **No Direct Database Access**: All database operations go through repository
5. **No Direct External Calls**: All external calls go through gateway layer











