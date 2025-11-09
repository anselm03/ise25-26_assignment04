# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

# V 0.1

## Added

- n/a

## Changed

- Fix broken test case in `PosSystemTests` (assignment 3)
- Extend GitHub Actions triggers to include pushes to feature branches (assignment 3)
- Add new `POST` endpoint `/api/pos/import/osm/{nodeId}` that allows API users to import a `POS` based on an OpenStreetMap node
- Extend `PosService` interface by adding a `importFromOsmNode` method

## Removed

- n/a

# V 0.11

## Added

- **Complete OSM Import Feature Implementation**:
    - Implemented real HTTP client in `OsmDataServiceImpl` using Spring's `RestClient` to fetch data from OpenStreetMap API
    - Extended `OsmNode` record with all relevant fields: coordinates, name, amenity, address components, website, phone, opening hours
    - Implemented `convertOsmNodeToPos()` with full OSM tag to POS mapping logic
    - Added postal code to campus mapping: 69115→BERGHEIM, 69117→ALTSTADT, 69120→INF, others→null
    - Added amenity type mapping: cafe, bakery, vending_machine, cafeteria/restaurant/fast_food
    - Enhanced `OsmNodeMissingFieldsException` to list specific missing fields
    - Added comprehensive field validation with detailed error messages
    - Automatic description generation from OSM metadata

## Changed

- Add example of new OSM import endpoint to `README` file
- Changed `Pos.campus` field from `@NonNull` to `@Nullable` to support unmapped postal codes per requirements
- Removed hardcoded stub implementation from `OsmDataServiceImpl` and `PosServiceImpl.convertOsmNodeToPos()`

## Removed 

- n/a