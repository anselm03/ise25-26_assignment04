# Project Requirement Proposal (PRP)
<!-- Adapted from https://github.com/Wirasm/PRPs-agentic-eng/tree/development/PRPs -->

You are a senior software engineer.
Use the information below to implement a new feature or improvement in this software project.

## Goal

**Feature Goal**: Enable import of Point of Sale (POS) data from OpenStreetMap by providing an OSM node ID through the existing HTTP endpoint POST /api/pos/import/osm/{nodeId}

**Deliverable**: Fully implemented service layer that fetches OSM node data via OpenStreetMap API, parses the XML response, maps OSM tags to POS entity fields, and persists the POS to the database

**Success Definition**: A valid OSM node ID (e.g., 5589879349) submitted to POST /api/pos/import/osm/{nodeId} successfully creates and returns a new POS entry with data populated from OpenStreetMap, including name, coordinates, address, and other available attributes

## User Persona (if applicable)

**Target User**: Developer or system integrator building crowdsourcing features for collecting cafe data near universities

**Use Case**: Importing a Cafe/Location via OSM-Node

**User Journey**: 
1. User identifies a cafe on OpenStreetMap (e.g., browsing the map or using OSM search)
2. User copies the OSM node ID from the URL or OSM interface (e.g., 5589879349)
3. User makes HTTP POST request to /api/pos/import/osm/5589879349
4. System fetches data from OpenStreetMap API
5. System parses and validates the OSM data
6. System creates new POS entry in database
7. User receives HTTP 201 response with the newly created POS object including all imported data

**Pain Points Addressed**: Eliminates the need to manually construct JSON payloads with all POS details when the location already exists in OpenStreetMap infrastructure, reducing data entry errors and saving time

## Why

- Leverages existing, high-quality OpenStreetMap data instead of requiring manual data entry for each cafe
- Enables rapid expansion of the POS database by utilizing community-maintained OSM infrastructure
- Reduces barrier to entry for developers implementing crowdsourcing features by providing a simple import mechanism via OSM node ID
- Solves the problem of duplicate data entry and inconsistent formatting that occurs with manual JSON payload creation

## What

The system accepts an OSM node ID via the existing POST /api/pos/import/osm/{nodeId} endpoint, retrieves the corresponding node data from https://api.openstreetmap.org/api/0.6/node/{nodeId}, parses the XML response to extract relevant cafe information (name, coordinates, address fields, opening hours, contact details), maps these OSM tags to the POS entity structure, validates that required fields are present, and persists the new POS to the database.

**Campus Name Mapping**: The system shall derive the campus name from the postal code (PLZ) using the following mapping rules:
- PLZ near Altstadt (e.g., 69117) → Campus: "Altstadt"
- PLZ near INF (e.g., 69120) → Campus: "INF"
- PLZ near Bergheim (e.g., 69115) → Campus: "Bergheim"
- If PLZ is not available or cannot be mapped, campus field should be set to null

### Success Criteria

- [ ] POST /api/pos/import/osm/{nodeId} successfully fetches data from OSM API for valid node IDs
- [ ] OSM XML response is correctly parsed to extract node attributes (lat, lon) and tag key-value pairs
- [ ] OSM tags are properly mapped to POS entity fields (name, coordinates, address components, opening hours, website, phone)
- [ ] POS entity is validated and persisted to PostgreSQL database
- [ ] HTTP 201 response returns the created POS object with all imported data
- [ ] Error handling returns appropriate HTTP status codes: 404 for non-existent nodes, 400 for invalid node IDs, 500 for API/parsing errors
- [ ] Integration test successfully imports the example node 5589879349 (Rada Coffee & Rösterei)

## Documentation & References

MUST READ - Include the following information in your context window.

The `README.md` file at the root of the project contains setup instructions and example API calls.

This Java Spring Boot application is structured as a multi-module Maven project following the ports-and-adapters architectural pattern.
There are the following submodules:

`api` - Maven submodule for controller adapter.

`application` - Maven submodule for Spring Boot application, test data import, and system tests.

`data` - Maven submodule for data adapter.

`domain` - Maven submodule for domain model, main business logic, and ports.