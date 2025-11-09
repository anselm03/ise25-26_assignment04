package de.seuhd.campuscoffee.domain.exceptions;

import java.util.List;

/**
 * Exception thrown when an OpenStreetMap node does not contain the fields required to create a POS.
 */
public class OsmNodeMissingFieldsException extends RuntimeException {
    public OsmNodeMissingFieldsException(Long nodeId) {
        super("The OpenStreetMap node with ID " + nodeId + " does not have the required fields.");
    }

    public OsmNodeMissingFieldsException(Long nodeId, List<String> missingFields) {
        super("The OpenStreetMap node with ID " + nodeId + " is missing required fields: "
                + String.join(", ", missingFields));
    }
}
