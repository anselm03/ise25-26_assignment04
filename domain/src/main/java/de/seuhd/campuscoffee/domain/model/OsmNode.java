package de.seuhd.campuscoffee.domain.model;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents an OpenStreetMap node with relevant Point of Sale information.
 * This is the domain model for OSM data before it is converted to a POS object.
 *
 * @param nodeId      The OpenStreetMap node ID
 * @param latitude    The latitude coordinate
 * @param longitude   The longitude coordinate
 * @param name        The name from OSM tags (e.g., "Rada Coffee &amp; Rösterei")
 * @param amenity     The amenity type (e.g., "cafe", "vending_machine")
 * @param street      Street name from addr:street tag
 * @param houseNumber House number from addr:housenumber tag
 * @param postalCode  Postal code from addr:postcode tag
 * @param city        City from addr:city tag
 * @param website     Website URL (optional)
 * @param phone       Phone number (optional)
 * @param openingHours Opening hours (optional)
 */
@Builder
public record OsmNode(
        @NonNull Long nodeId,
        @Nullable Double latitude,
        @Nullable Double longitude,
        @Nullable String name,
        @Nullable String amenity,
        @Nullable String street,
        @Nullable String houseNumber,
        @Nullable String postalCode,
        @Nullable String city,
        @Nullable String website,
        @Nullable String phone,
        @Nullable String openingHours
) {
}
