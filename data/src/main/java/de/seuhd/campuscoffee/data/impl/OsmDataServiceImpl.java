package de.seuhd.campuscoffee.data.impl;

import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.ports.OsmDataService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * OSM import service that fetches real data from the OpenStreetMap API.
 */
@Service
@Slf4j
class OsmDataServiceImpl implements OsmDataService {

    private static final String OSM_API_BASE_URL = "https://api.openstreetmap.org/api/0.6";
    private final RestClient restClient;

    public OsmDataServiceImpl() {
        this.restClient = RestClient.builder()
                .baseUrl(OSM_API_BASE_URL)
                .defaultHeader("User-Agent", "CampusCoffee/0.0.1 (University Project)")
                .build();
    }

    @Override
    public @NonNull OsmNode fetchNode(@NonNull Long nodeId) throws OsmNodeNotFoundException {
        log.info("Fetching OSM node {} from OpenStreetMap API", nodeId);

        try {
            // Fetch XML from OSM API
            String xmlResponse = restClient.get()
                    .uri("/node/{id}", nodeId)
                    .retrieve()
                    .body(String.class);

            if (xmlResponse == null) {
                throw new OsmNodeNotFoundException(nodeId);
            }

            // Parse the XML response
            OsmNode osmNode = parseOsmXml(nodeId, xmlResponse);
            log.info("Successfully fetched OSM node {}: {}", nodeId, osmNode.name());
            return osmNode;

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("OSM node {} not found", nodeId);
            throw new OsmNodeNotFoundException(nodeId);
        } catch (Exception e) {
            log.error("Error fetching OSM node {}: {}", nodeId, e.getMessage(), e);
            throw new OsmNodeNotFoundException(nodeId);
        }
    }

    /**
     * Parses OSM XML response and extracts node data.
     *
     * @param nodeId the node ID being parsed
     * @param xmlContent the XML content from OSM API
     * @return OsmNode with extracted data
     */
    private OsmNode parseOsmXml(Long nodeId, String xmlContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes()));

        // Get the <node> element
        NodeList nodeList = doc.getElementsByTagName("node");
        if (nodeList.getLength() == 0) {
            throw new OsmNodeNotFoundException(nodeId);
        }

        Element nodeElement = (Element) nodeList.item(0);

        // Extract coordinates from node attributes
        Double latitude = parseDouble(nodeElement.getAttribute("lat"));
        Double longitude = parseDouble(nodeElement.getAttribute("lon"));

        // Extract all tags into a map
        Map<String, String> tags = extractTags(nodeElement);

        // Build OsmNode from parsed data
        return OsmNode.builder()
                .nodeId(nodeId)
                .latitude(latitude)
                .longitude(longitude)
                .name(tags.get("name"))
                .amenity(tags.get("amenity"))
                .street(tags.get("addr:street"))
                .houseNumber(tags.get("addr:housenumber"))
                .postalCode(tags.get("addr:postcode"))
                .city(tags.get("addr:city"))
                .website(tags.get("website"))
                .phone(tags.get("phone"))
                .openingHours(tags.get("opening_hours"))
                .build();
    }

    /**
     * Extracts all tag key-value pairs from the node element.
     *
     * @param nodeElement the XML node element
     * @return map of tag keys to values
     */
    private Map<String, String> extractTags(Element nodeElement) {
        Map<String, String> tags = new HashMap<>();
        NodeList tagList = nodeElement.getElementsByTagName("tag");

        for (int i = 0; i < tagList.getLength(); i++) {
            Element tagElement = (Element) tagList.item(i);
            String key = tagElement.getAttribute("k");
            String value = tagElement.getAttribute("v");
            tags.put(key, value);
        }

        log.debug("Extracted {} tags from OSM node", tags.size());
        return tags;
    }

    /**
     * Safely parses a string to Double, returning null if invalid.
     */
    private Double parseDouble(String value) {
        try {
            return value != null && !value.isEmpty() ? Double.parseDouble(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
