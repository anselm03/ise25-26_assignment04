# OSM Import Feature - API Usage Examples

## Endpoint
```
POST /api/pos/import/osm/{nodeId}
```

## Example 1: Successful Import (Rada Coffee & Rösterei)

### Request
```bash
curl -X POST http://localhost:8080/api/pos/import/osm/5589879349 \
  -H "Accept: application/json"
```

### Expected Response
```http
HTTP/1.1 201 Created
Location: /api/pos/5
Content-Type: application/json

{
  "id": 5,
  "createdAt": "2025-11-08T18:45:00.123",
  "updatedAt": "2025-11-08T18:45:00.123",
  "name": "Rada Coffee & Rösterei",
  "description": "Cafe - Mo-Fr 08:00-18:00; Sa 09:00-18:00",
  "type": "CAFE",
  "campus": "ALTSTADT",
  "street": "Untere Straße",
  "houseNumber": "21",
  "postalCode": 69117,
  "city": "Heidelberg"
}
```

### Campus Mapping
- Postal code `69117` → Campus: `ALTSTADT`

---

## Example 2: Missing Required Fields

### Request
```bash
curl -X POST http://localhost:8080/api/pos/import/osm/999999999 \
  -H "Accept: application/json"
```

### Expected Response
```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "errorCode": "OsmNodeMissingFieldsException",
  "message": "The OpenStreetMap node with ID 999999999 is missing required fields: name, addr:street, addr:housenumber",
  "statusCode": 400,
  "statusMessage": "Bad Request",
  "timestamp": "2025-11-08T18:45:00.123",
  "path": "/api/pos/import/osm/999999999"
}
```

---

## Example 3: Non-existent OSM Node

### Request
```bash
curl -X POST http://localhost:8080/api/pos/import/osm/1 \
  -H "Accept: application/json"
```

### Expected Response
```http
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "errorCode": "OsmNodeNotFoundException",
  "message": "The OpenStreetMap node with ID 1 does not exist.",
  "statusCode": 404,
  "statusMessage": "Not Found",
  "timestamp": "2025-11-08T18:45:00.123",
  "path": "/api/pos/import/osm/1"
}
```

---

## Example 4: Duplicate POS Name

If a POS with the same name already exists:

### Request
```bash
# Import the same node twice
curl -X POST http://localhost:8080/api/pos/import/osm/5589879349
curl -X POST http://localhost:8080/api/pos/import/osm/5589879349
```

### Expected Response (second request)
```http
HTTP/1.1 409 Conflict
Content-Type: application/json

{
  "errorCode": "DuplicatePosNameException",
  "message": "A POS with the name 'Rada Coffee & Rösterei' already exists.",
  "statusCode": 409,
  "statusMessage": "Conflict",
  "timestamp": "2025-11-08T18:45:00.123",
  "path": "/api/pos/import/osm/5589879349"
}
```

---

## Example 5: Campus Mapping Examples

### Bergheim Campus (PLZ 69115)
```bash
# Any OSM node in 69115 postal code → campus: "BERGHEIM"
curl -X POST http://localhost:8080/api/pos/import/osm/{nodeId}
```

### INF Campus (PLZ 69120)
```bash
# Any OSM node in 69120 postal code → campus: "INF"
curl -X POST http://localhost:8080/api/pos/import/osm/{nodeId}
```

### Unmapped Postal Code
```bash
# Any OSM node with unmapped postal code → campus: null
curl -X POST http://localhost:8080/api/pos/import/osm/{nodeId}

# Response will have:
{
  ...
  "campus": null,
  "postalCode": 12345,
  ...
}
```

---

## Amenity Type Mapping

The following OSM `amenity` tags are mapped to POS types:

| OSM Amenity | POS Type | Example |
|-------------|----------|---------|
| `cafe` | `CAFE` | Coffee shops, cafés |
| `bakery` | `BAKERY` | Bakeries selling coffee |
| `vending_machine` | `VENDING_MACHINE` | Coffee vending machines |
| `cafeteria` | `CAFETERIA` | University cafeterias |
| `restaurant` | `CAFETERIA` | Restaurants (if they serve coffee) |
| `fast_food` | `CAFETERIA` | Fast food with coffee |
| Unknown | `CAFE` | Defaults to CAFE with warning |

---

## How to Find OSM Node IDs

1. **OpenStreetMap Website**:
   - Go to https://www.openstreetmap.org
   - Search for a location
   - Click on the point of interest
   - The URL will contain the node ID: `https://www.openstreetmap.org/node/{nodeId}`

2. **Example Nodes** (Heidelberg):
   - Rada Coffee & Rösterei: `5589879349`
   - (Find more at openstreetmap.org)

3. **OSM API Direct**:
   - View XML: `https://www.openstreetmap.org/api/0.6/node/5589879349`

---

## Testing Workflow

### Setup
```bash
# 1. Start PostgreSQL
docker run -d \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:17-alpine

# 2. Start the application
cd application
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Test Import
```bash
# Import a cafe
curl -X POST http://localhost:8080/api/pos/import/osm/5589879349

# Verify it was created
curl http://localhost:8080/api/pos

# Check specific POS
curl http://localhost:8080/api/pos/5
```

### Cleanup (if needed)
```bash
# Delete a POS (if you want to re-import)
# Note: DELETE endpoint may not exist in current implementation
# Alternatively, restart the app with dev profile to reset data
```

---

## Notes

- **Required Fields**: name, amenity, addr:street, addr:housenumber, addr:postcode, addr:city
- **Optional Fields**: website, phone, opening_hours (used in description)
- **Validation**: All required fields must be present in OSM data
- **Postal Code**: Must be a valid integer
- **Description**: Auto-generated from amenity type, opening hours, and website
- **Timestamps**: Automatically set by JPA lifecycle callbacks
- **Location Header**: Returns URL to the created resource

---

## Error Summary

| Status | Error | Reason |
|--------|-------|--------|
| 201 | Success | POS created |
| 400 | Bad Request | Missing required OSM fields |
| 404 | Not Found | OSM node doesn't exist |
| 409 | Conflict | POS name already exists |
| 500 | Server Error | Unexpected error |

