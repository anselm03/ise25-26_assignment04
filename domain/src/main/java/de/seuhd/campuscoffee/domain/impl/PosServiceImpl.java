package de.seuhd.campuscoffee.domain.impl;

import de.seuhd.campuscoffee.domain.exceptions.DuplicatePosNameException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeMissingFieldsException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.model.CampusType;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.model.Pos;
import de.seuhd.campuscoffee.domain.exceptions.PosNotFoundException;
import de.seuhd.campuscoffee.domain.model.PosType;
import de.seuhd.campuscoffee.domain.ports.OsmDataService;
import de.seuhd.campuscoffee.domain.ports.PosDataService;
import de.seuhd.campuscoffee.domain.ports.PosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of the POS service that handles business logic related to POS entities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PosServiceImpl implements PosService {
    private final PosDataService posDataService;
    private final OsmDataService osmDataService;

    @Override
    public void clear() {
        log.warn("Clearing all POS data");
        posDataService.clear();
    }

    @Override
    public @NonNull List<Pos> getAll() {
        log.debug("Retrieving all POS");
        return posDataService.getAll();
    }

    @Override
    public @NonNull Pos getById(@NonNull Long id) throws PosNotFoundException {
        log.debug("Retrieving POS with ID: {}", id);
        return posDataService.getById(id);
    }

    @Override
    public @NonNull Pos upsert(@NonNull Pos pos) throws PosNotFoundException {
        if (pos.id() == null) {
            // Create new POS
            log.info("Creating new POS: {}", pos.name());
            return performUpsert(pos);
        } else {
            // Update existing POS
            log.info("Updating POS with ID: {}", pos.id());
            // POS ID must be set
            Objects.requireNonNull(pos.id());
            // POS must exist in the database before the update
            posDataService.getById(pos.id());
            return performUpsert(pos);
        }
    }

    @Override
    public @NonNull Pos importFromOsmNode(@NonNull Long nodeId) throws OsmNodeNotFoundException {
        log.info("Importing POS from OpenStreetMap node {}...", nodeId);

        // Fetch the OSM node data using the port
        OsmNode osmNode = osmDataService.fetchNode(nodeId);

        // Convert OSM node to POS domain object and upsert it
        Pos savedPos = upsert(convertOsmNodeToPos(osmNode));
        log.info("Successfully imported POS '{}' from OSM node {}", savedPos.name(), nodeId);

        return savedPos;
    }

    /**
     * Converts an OSM node to a POS domain object.
     * Validates that all required fields are present and maps OSM data to POS structure.
     *
     * @param osmNode the OSM node data to convert
     * @return POS domain object ready for persistence
     * @throws OsmNodeMissingFieldsException if required fields are missing
     */
    private @NonNull Pos convertOsmNodeToPos(@NonNull OsmNode osmNode) {
        // Validate required fields
        List<String> missingFields = new ArrayList<>();

        if (osmNode.name() == null || osmNode.name().isBlank()) {
            missingFields.add("name");
        }
        if (osmNode.amenity() == null || osmNode.amenity().isBlank()) {
            missingFields.add("amenity");
        }
        if (osmNode.street() == null || osmNode.street().isBlank()) {
            missingFields.add("addr:street");
        }
        if (osmNode.houseNumber() == null || osmNode.houseNumber().isBlank()) {
            missingFields.add("addr:housenumber");
        }
        if (osmNode.postalCode() == null || osmNode.postalCode().isBlank()) {
            missingFields.add("addr:postcode");
        }
        if (osmNode.city() == null || osmNode.city().isBlank()) {
            missingFields.add("addr:city");
        }

        if (!missingFields.isEmpty()) {
            log.warn("OSM node {} is missing required fields: {}", osmNode.nodeId(), missingFields);
            throw new OsmNodeMissingFieldsException(osmNode.nodeId(), missingFields);
        }

        // Map amenity type to PosType
        PosType posType = mapAmenityToPosType(osmNode.amenity());

        // Parse postal code
        Integer postalCodeInt = parsePostalCode(osmNode.postalCode());
        if (postalCodeInt == null) {
            missingFields.add("addr:postcode (invalid format)");
            throw new OsmNodeMissingFieldsException(osmNode.nodeId(), missingFields);
        }

        // Map postal code to campus
        CampusType campus = mapPostalCodeToCampus(postalCodeInt);

        // Build description from available data
        String description = buildDescription(osmNode);

        log.debug("Converting OSM node {} to POS: name='{}', type={}, campus={}",
                osmNode.nodeId(), osmNode.name(), posType, campus);

        return Pos.builder()
                .name(osmNode.name())
                .description(description)
                .type(posType)
                .campus(campus)
                .street(osmNode.street())
                .houseNumber(osmNode.houseNumber())
                .postalCode(postalCodeInt)
                .city(osmNode.city())
                .build();
    }

    /**
     * Maps OSM amenity tag to PosType enum.
     *
     * @param amenity the OSM amenity value
     * @return corresponding PosType
     */
    private PosType mapAmenityToPosType(String amenity) {
        return switch (amenity.toLowerCase()) {
            case "cafe" -> PosType.CAFE;
            case "bakery" -> PosType.BAKERY;
            case "vending_machine" -> PosType.VENDING_MACHINE;
            case "cafeteria", "restaurant", "fast_food" -> PosType.CAFETERIA;
            default -> {
                log.warn("Unknown amenity type '{}', defaulting to CAFE", amenity);
                yield PosType.CAFE;
            }
        };
    }

    /**
     * Maps postal code to campus location based on Heidelberg university campus areas.
     *
     * Mapping rules:
     * - 69115: Bergheim
     * - 69117: Altstadt
     * - 69120: INF (Im Neuenheimer Feld)
     * - Unknown: null
     *
     * @param postalCode the postal code
     * @return corresponding CampusType or null if unmapped
     */
    private @Nullable CampusType mapPostalCodeToCampus(Integer postalCode) {
        return switch (postalCode) {
            case 69115 -> CampusType.BERGHEIM;
            case 69117 -> CampusType.ALTSTADT;
            case 69120 -> CampusType.INF;
            default -> {
                log.warn("Postal code {} does not map to any known campus, setting campus to null", postalCode);
                yield null;
            }
        };
    }

    /**
     * Parses postal code string to integer.
     *
     * @param postalCodeStr the postal code as string
     * @return postal code as integer, or null if invalid
     */
    private @Nullable Integer parsePostalCode(String postalCodeStr) {
        try {
            return Integer.parseInt(postalCodeStr.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid postal code format: '{}'", postalCodeStr);
            return null;
        }
    }

    /**
     * Builds a description from available OSM data.
     *
     * @param osmNode the OSM node
     * @return description string
     */
    private String buildDescription(OsmNode osmNode) {
        StringBuilder desc = new StringBuilder();

        if (osmNode.amenity() != null) {
            desc.append(capitalizeFirst(osmNode.amenity()));
        }

        if (osmNode.openingHours() != null && !osmNode.openingHours().isBlank()) {
            if (!desc.isEmpty()) {
                desc.append(" - ");
            }
            desc.append("Hours: ").append(osmNode.openingHours());
        }

        if (osmNode.website() != null && !osmNode.website().isBlank()) {
            if (!desc.isEmpty()) {
                desc.append(" - ");
            }
            desc.append(osmNode.website());
        }

        // If no description could be built, use a default
        if (desc.isEmpty()) {
            desc.append("Imported from OpenStreetMap (node ").append(osmNode.nodeId()).append(")");
        }

        return desc.toString();
    }

    /**
     * Capitalizes the first character of a string.
     */
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Performs the actual upsert operation with consistent error handling and logging.
     * Database constraint enforces name uniqueness - data layer will throw DuplicatePosNameException if violated.
     * JPA lifecycle callbacks (@PrePersist/@PreUpdate) set timestamps automatically.
     *
     * @param pos the POS to upsert
     * @return the persisted POS with updated ID and timestamps
     * @throws DuplicatePosNameException if a POS with the same name already exists
     */
    private @NonNull Pos performUpsert(@NonNull Pos pos) throws DuplicatePosNameException {
        try {
            Pos upsertedPos = posDataService.upsert(pos);
            log.info("Successfully upserted POS with ID: {}", upsertedPos.id());
            return upsertedPos;
        } catch (DuplicatePosNameException e) {
            log.error("Error upserting POS '{}': {}", pos.name(), e.getMessage());
            throw e;
        }
    }
}
