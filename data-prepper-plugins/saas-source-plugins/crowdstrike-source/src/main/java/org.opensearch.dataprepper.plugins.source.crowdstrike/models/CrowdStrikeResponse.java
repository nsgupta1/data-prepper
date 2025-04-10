package org.opensearch.dataprepper.plugins.source.crowdstrike.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

public class CrowdStrikeResponse {

    @Getter @Setter
    private CrowdStrikeSearchResults body;
    @Getter @Setter
    private Map<String, List<String>> headers;


    // Convenience method to get a specific header
    public List<String> getHeader(String headerName) {
        return headers.getOrDefault(headerName, Collections.emptyList());
    }

    // Convenience method to get the first value of a specific header
    public Optional<String> getFirstHeaderValue(String headerName) {
        List<String> values = headers.get(headerName);
        return values != null && !values.isEmpty() ? Optional.of(values.get(0)) : Optional.empty();
    }
}
