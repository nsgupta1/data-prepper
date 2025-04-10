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

    @JsonProperty("limit")
    private Integer limit = null;

    @JsonProperty("total")
    private Integer total = null;

    @JsonProperty("resources")
    private List<CrowdStrikeItem> results = null;

    @JsonProperty("Next-Page")
    private String link = null;
}
