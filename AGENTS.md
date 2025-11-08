# AGENTS.md - CampusCoffee AI Assistant Context

## 1. Project Overview & Tech Stack

**Project Name:** CampusCoffee
**Description:** Spring Boot application for managing Points of Sale (POS) for coffee shops and cafés on university campuses, with OpenStreetMap (OSM) integration.

**Core Technologies:**
- **Language:** Java 21
- **Framework:** Spring Boot 3.5.7
- **Build Tool:** Maven 3.9
- **Database:** PostgreSQL 17
- **ORM:** JPA/Hibernate
- **Containerization:** Docker
- **Testing:** JUnit 5, Mockito
- **Code Generation:** Lombok, MapStruct
- **API Documentation:** Swagger/OpenAPI (configured)

**Architecture Pattern:** Hexagonal Architecture (Ports & Adapters)
- Domain layer defines business logic and ports
- Application layer orchestrates startup
- API layer provides REST endpoints
- Data layer implements persistence adapters

---

## 2. Existing Project Structure

```
campuscoffee/
├── domain/                          # Core business logic (Framework-independent)
│   ├── src/main/java/
│   │   └── de/seuhd/campuscoffee/domain/
│   │       ├── model/               # Domain objects (immutable records)
│   │       │   ├── Pos.java         # Domain record for Point of Sale
│   │       │   ├── OsmNode.java     # Domain record for OSM data
│   │       │   ├── PosType.java     # Enum: CAFE, BAKERY, etc.
│   │       │   ├── CampusType.java  # Enum: ALTSTADT, INF, BERGHEIM
│   │       ├── ports/               # Interfaces (contracts for adapters)
│   │       │   ├── PosService.java
│   │       │   ├── PosDataService.java
│   │       │   ├── OsmDataService.java
│   │       ├── impl/                # Service implementations (business logic)
│   │       │   ├── PosServiceImpl.java
│   │       ├── exceptions/          # Domain-specific exceptions
│   │       │   ├── PosNotFoundException.java
│   │       │   ├── DuplicatePosNameException.java
│   │       │   ├── OsmNodeNotFoundException.java
│   │       │   ├── OsmNodeMissingFieldsException.java
│   │       ├── tests/               # Test fixtures
│   │       │   └── TestFixtures.java
│
├── data/                            # Persistence layer (implements domain ports)
│   ├── src/main/java/
│   │   └── de/seuhd/campuscoffee/data/
│   │       ├── impl/                # Adapter implementations
│   │       │   ├── PosDataServiceImpl.java
│   │       │   ├── OsmDataServiceImpl.java (TODO: Real HTTP client)
│   │       ├── persistence/         # JPA entities & repositories
│   │       │   ├── PosEntity.java   # @Entity mapped to DB table
│   │       │   ├── AddressEntity.java # @Embeddable address data
│   │       │   ├── PosRepository.java
│   │       ├── mapper/              # MapStruct entity-to-domain conversion
│   │       │   ├── PosEntityMapper.java
│   ├── src/main/resources/
│   │   └── db/migration/
│   │       └── V1__create_pos_table.sql # Flyway migrations
│
├── api/                             # REST API layer
│   ├── src/main/java/
│   │   └── de/seuhd/campuscoffee/api/
│   │       ├── controller/          # REST endpoints
│   │       │   └── PosController.java
│   │       ├── dtos/                # Data Transfer Objects
│   │       │   └── PosDto.java
│   │       ├── mapper/              # MapStruct domain-to-DTO conversion
│   │       │   └── PosDtoMapper.java
│   │       ├── exceptions/          # API error handling
│   │       │   ├── GlobalExceptionHandler.java
│   │       │   └── ErrorResponse.java
│
├── application/                     # Spring Boot main app
│   ├── src/main/java/
│   │   └── de/seuhd/campuscoffee/
│   │       ├── Application.java     # @SpringBootApplication entry point
│   │       └── LoadInitialData.java # Dev profile data loader
│   ├── src/main/resources/
│   │   ├── application.yaml         # Spring config (dev/prod profiles)
│   │   └── logback-spring.xml       # Logging config
│
├── pom.xml                          # Maven configuration (parent POM)
├── README.md                        # User documentation
├── CHANGELOG.md                     # Version history
└── AGENTS.md                        # This file (AI context)
```

---

## 3. Code Style Guidelines

### Naming Conventions

**Classes & Records:**
- `PascalCase` for all class names
- Records use `@Builder` for construction: `Pos.builder().name("...").build()`
- Entities use `*Entity` suffix: `PosEntity`, `AddressEntity`
- DTOs use `*Dto` suffix: `PosDto`
- Mappers use `*Mapper` suffix: `PosDtoMapper`, `PosEntityMapper`
- Interfaces (Ports) use no suffix: `PosService`, `PosDataService`
- Implementations append `Impl`: `PosServiceImpl`, `PosDataServiceImpl`

**Methods & Variables:**
- `camelCase` for all methods and variables
- Boolean getters use prefix: `isDuplicate()`, `isEmpty()`
- Test methods use descriptive names: `testValidPosImportFromOsm()`

**Constants:**
- `UPPER_SNAKE_CASE` for static final constants
- Example: `POS_NAME_CONSTRAINT = "pos_name_key"`

### Annotations & Decorators

**Class-Level:**
```java
@Service                   // Spring bean registration
@RequiredArgsConstructor   // Lombok: final fields → constructor injection
@Slf4j                     // Lombok: adds 'log' logger field
@Mapper(componentModel = "spring")  // MapStruct: generates Spring bean
@Entity / @Embeddable      // JPA entity/value object markers
```

**Method-Level:**
```java
@Override                  // Always used when implementing interface methods
@NonNull / @Nullable       // JSpecify: explicit null-safety documentation
@PrePersist / @PreUpdate   // JPA lifecycle callbacks for timestamps
@Mapping / @Mappings       // MapStruct field mappings
@ExceptionHandler          // Spring: centralized exception handling
```

### Null Safety

- All public methods document nullability with `@NonNull` / `@Nullable`
- Example:
  ```java
  @NonNull Pos getById(@NonNull Long id) throws PosNotFoundException;
  ```
- DTOs and domain records use `@NonNull` / `@Nullable` on record fields
- `Objects.requireNonNull()` for defensive checks in implementations

### Exception Handling

**Domain Layer:**
- Custom exceptions extend `RuntimeException`
- Exceptions include descriptive messages with context
- Example: `new PosNotFoundException(id)`
- Thrown from domain services and caught by API layer

**API Layer:**
- `GlobalExceptionHandler` provides centralized exception mapping
- Returns `ErrorResponse` with standardized JSON structure:
  ```java
  {
    "errorCode": "PosNotFoundException",
    "message": "POS with ID 999 does not exist.",
    "statusCode": 404,
    "statusMessage": "Not Found",
    "timestamp": "2025-01-15T10:30:45",
    "path": "/api/pos/999"
  }
  ```

### Record Usage

All immutable domain objects use records (not classes):
```java
@Builder(toBuilder = true)
public record Pos(
    @Nullable Long id,
    @NonNull String name,
    @NonNull PosType type,
    @NonNull CampusType campus,
    ...
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
```

Benefits: auto `equals()`, `hashCode()`, `toString()`, immutability

### Lombok Usage

**Preferred Annotations:**
- `@Slf4j` for logger injection
- `@RequiredArgsConstructor` for constructor DI
- `@Builder` for fluent object construction
- `@Getter`, `@Setter` on entities (JPA requires mutable fields)
- `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` for selective comparison

**Not Used:**
- `@Data` (combines too many concerns, use `@Builder` + records instead)
- `@AllArgsConstructor` on services (use `@RequiredArgsConstructor` instead)

---

## 4. Common Patterns

### Hexagonal Architecture (Ports & Adapters)

**Pattern Structure:**
1. **Domain Layer** defines `Port` interfaces (contracts)
2. **Application/API Layer** depends on ports
3. **Data/External Layer** implements ports as adapters

**Concrete Example:**
```java
// DOMAIN: Port (interface)
public interface PosDataService {
    @NonNull Pos upsert(@NonNull Pos pos);
}

// DATA: Adapter (implementation)
@Service
class PosDataServiceImpl implements PosDataService {
    public @NonNull Pos upsert(@NonNull Pos pos) {
        // Persistence logic
    }
}

// DOMAIN: Consumer (uses port via dependency injection)
@Service
class PosServiceImpl {
    private final PosDataService posDataService;  // Injected port
    public @NonNull Pos importFromOsmNode(@NonNull Long nodeId) {
        OsmNode osmNode = osmDataService.fetchNode(nodeId);
        return posDataService.upsert(convertOsmNodeToPos(osmNode));
    }
}
```

### Dependency Injection

All dependencies injected via constructor with `@RequiredArgsConstructor`:
```java
@Service
@RequiredArgsConstructor
class PosServiceImpl implements PosService {
    private final PosDataService posDataService;
    private final OsmDataService osmDataService;
    // Constructor auto-generated by Lombok
}
```

### MapStruct Mapping

**Two-Layer Mapping Pattern:**
1. **Entity ↔ Domain:** `PosEntityMapper` (JPA ↔ Business Model)
2. **Domain ↔ DTO:** `PosDtoMapper` (Business ↔ API Response)

**Example Mapper:**
```java
@Mapper(componentModel = "spring")
public interface PosEntityMapper {
    @Mapping(source = "address.street", target = "street")
    @Mapping(target = "houseNumber", expression = "java(...)")
    Pos fromEntity(PosEntity source);

    PosEntity toEntity(Pos source);

    void updateEntity(Pos source, @MappingTarget PosEntity target);
}
```

**Key Features:**
- Automatic field mapping when names match
- `@Mapping` for field transformation
- Expression language for complex logic: `expression = "java(...)"`
- `updateEntity()` preserves JPA-managed fields (id, timestamps)

### JPA Lifecycle Callbacks

Timestamps managed automatically:
```java
@Entity
public class PosEntity {
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneId.of("UTC"));
    }
}
```

### Embeddable Value Objects

Address split across multiple fields in database:
```java
@Embeddable
public class AddressEntity {
    private String street;
    @Column(name = "house_number")
    private Integer houseNumber;
    @Column(name = "house_number_suffix")
    private Character houseNumberSuffix;
    private Integer postalCode;
    private String city;
}

@Entity
public class PosEntity {
    @Embedded
    private AddressEntity address;
}
```

### Upsert Pattern (Create or Update)

Used in both service and data layers:
```java
@Override
public @NonNull Pos upsert(@NonNull Pos pos) {
    if (pos.id() == null) {
        // CREATE: new POS
        return posDataService.upsert(pos);  // DB assigns ID
    } else {
        // UPDATE: existing POS
        posDataService.getById(pos.id());   // Verify exists
        return posDataService.upsert(pos);  // Update fields
    }
}
```

### Global Exception Handler

Centralized exception mapping to HTTP responses:
```java
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler({PosNotFoundException.class, OsmNodeNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFoundException(
        RuntimeException exception,
        WebRequest request
    ) {
        return buildErrorResponse(exception, HttpStatus.NOT_FOUND, request);
    }
}
```

**HTTP Status Mapping:**
- `404 Not Found`: `PosNotFoundException`, `OsmNodeNotFoundException`
- `409 Conflict`: `DuplicatePosNameException`
- `400 Bad Request`: `IllegalArgumentException`, `OsmNodeMissingFieldsException`
- `500 Internal Server Error`: Unexpected exceptions

### REST Controller Pattern

```java
@Controller
@RequestMapping("/api/pos")
@RequiredArgsConstructor
public class PosController {
    private final PosService posService;
    private final PosDtoMapper posDtoMapper;

    @PostMapping("")
    public ResponseEntity<PosDto> create(@RequestBody PosDto posDto) {
        PosDto created = posDtoMapper.fromDomain(
            posService.upsert(posDtoMapper.toDomain(posDto))
        );
        return ResponseEntity
            .created(getLocation(created.id()))
            .body(created);
    }
}
```

**Key Patterns:**
- DTOs converted to domain models before service calls
- Domain results converted back to DTOs for responses
- `ResponseEntity.created()` for POST with location header
- `ResponseEntity.ok()` for GET/PUT

### Spring Profiles

Application supports multiple profiles:
```yaml
# application.yaml (default/base config)
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
  jpa:
    open-in-view: true

---
# dev profile (spring.config.activate.on-profile: dev)
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/postgres
    username: postgres
    password: postgres
server:
  error:
    include-message: always
```

Launch with: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`

### Flyway Database Migrations

Configuration in `application.yaml`:
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    validate-on-migrate: false
```

---

## 5. Dependencies & Libraries

### Core Spring Boot
- `spring-boot-starter-web` - REST API support
- `spring-boot-starter-data-jpa` - ORM/Hibernate
- `spring-boot-configuration-processor` - @ConfigurationProperties support

### Database
- PostgreSQL 17 JDBC driver (implicit via Spring Boot)
- Flyway - Database migration tool

### Mapping & Code Generation
- **Lombok** (v1.18.30+)
    - `@RequiredArgsConstructor` - Constructor injection
    - `@Slf4j` - Logger injection
    - `@Builder` - Fluent object building
    - `@Getter`, `@Setter` - JPA entity accessors
    - `@EqualsAndHashCode` - Selective comparison
- **MapStruct** (v1.6.3)
    - Auto-generates entity/DTO converters
    - Reduces boilerplate mapping code
- **JSpecify** (for null-safety annotations)

### Testing
- JUnit 5 (Jupiter)
- Mockito (for mocking)
- Spring Boot Test

### Utilities
- Apache Commons Lang3 (v3.19.0) - String utilities, SerializationUtils

### Annotations
- Jakarta Persistence (JPA 3.x) - `@Entity`, `@Column`, etc.
- Spring Framework - `@Service`, `@Controller`, `@RequiredArgsConstructor`, etc.

---

## 7. Development Workflow

### Build & Test
```shell
mvn clean install              # Full build with tests
mvn clean install -q           # Quiet mode
```

### Run Application
```shell
# Start PostgreSQL
docker run -d -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 postgres:17-alpine

# Start app (dev profile loads initial data)
cd application
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### REST API Examples
```shell
# Get all POS
curl http://localhost:8080/api/pos

# Get POS by ID
curl http://localhost:8080/api/pos/1

# Create POS from JSON
curl -H "Content-Type: application/json" -X POST \
  --data '{"name":"New Café","type":"CAFE","campus":"ALTSTADT",...}' \
  http://localhost:8080/api/pos

# Import from OSM
curl -X POST http://localhost:8080/api/pos/import/osm/5589879349
```

---

## 8. Notes for AI Assistant

**When Writing New Code:**
1. Use `@NonNull` / `@Nullable` on all public method parameters & returns
2. Add Javadoc for public classes & methods
3. Use records for immutable domain objects
4. Use `@RequiredArgsConstructor` for constructor injection
5. Leverage MapStruct for entity-DTO conversions
6. Throw domain exceptions from services
7. Let `GlobalExceptionHandler` handle exception-to-HTTP mapping
8. Place business logic in `*ServiceImpl` (domain layer)
9. Place data access in `*DataServiceImpl` (data layer)
10. Always use transactions (`@Transactional`) for write operations

**Code Generation Tools Active:**
- Lombok (annotations processed at compile time)
- MapStruct (generates mapper implementations)
- Spring annotation processor (for `@ConfigurationProperties`)

**Testing Fixtures:**
- Use `TestFixtures.getPosList()` for predefined test data
- Serialization cloning prevents test mutation issues

---

## 10. Known TODOs / Constraints
- `CampusType` enum incomplete (expand campus taxonomy).
- `OsmNode` needs fields: name, amenity, street, housenumber, postcode, city, opening_hours, coordinates.
- Real OSM fetch (HTTP client + parsing) missing; handle rate limits & errors.
- Generic OSM → POS mapping logic (amenity → `PosType`, address parsing, campus inference) not implemented.
- Logging config: `logback-spring.xml` (file + console); review levels for production.
- `open-in-view: true` may lead to unintended lazy loads; consider disabling with DTO projection adjustments.
- More validation (Bean Validation) could replace current implicit null checks.
- Add integration tests for OSM import flow (success, missing fields, duplicate name → update vs create).
- Extend error details for missing OSM tags (currently generic).

---

## Current Feature: OSM Import Implementation

### Existing Endpoint (Already Defined)
**Route:** `POST /api/pos/import/osm/{nodeId}`
**Path Parameter:** `nodeId` - OpenStreetMap Node ID (e.g., "5589879349")

### What Needs to be Implemented
The endpoint route exists, but the implementation is missing:

1. **OSM API Integration:**
    - Call `https://api.openstreetmap.org/api/0.6/node/{nodeId}`
    - Handle HTTP request/response

2. **XML Parsing:**
    - Parse OSM XML format
    - Extract node data and tags

3. **Data Mapping:**
    - Map OSM tags to POS entity fields

4. **Validation & Persistence:**
    - Validate required data
    - Save to database via existing service

---

### Feature Specification Placeholder
**Status:** Architektonisch vollständig, funktional als Stub

**Endpoint:**
```
POST /api/pos/import/osm/{nodeId}
Response: 201 Created, PosDto in body
```

**Known TODOs (blocking productiveness):**

1. **Real OSM HTTP Client** (`OsmDataServiceImpl.fetchNode()`)
    - Currently: Hardcoded for nodeId `5589879349L`
    - Needed: HTTP request to `https://www.openstreetmap.org/api/0.6/node/{id}`
    - Implementation: Use `RestTemplate` or Spring `WebClient`

2. **OsmNode Record Extension**
    - Currently: Only `nodeId` field
    - Needed: Extract OSM tags (name, amenity, address fields, opening_hours)

3. **Generic OSM → POS Conversion** (`PosServiceImpl.convertOsmNodeToPos()`)
    - Currently: Hardcoded response for one node ID
    - Needed: Map OSM amenity-type → PosType enum
    - Needed: Auto-detect campus from coordinates/address

**Error Handling:**
- `OsmNodeNotFoundException` (404) - Node doesn't exist
- `OsmNodeMissingFieldsException` (400) - Incomplete OSM data

---

END OF AGENTS.MD
