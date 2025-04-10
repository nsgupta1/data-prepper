package org.opensearch.dataprepper.plugins.source.crowdstrike.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * The result of FQL search.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrowdStrikeSearchResults {

    @JsonProperty("resources")
    private List<CrowdStrikeItem> results = null;

}
