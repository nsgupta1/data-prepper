package org.opensearch.dataprepper.plugins.source.crowdstrike.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CrowdStrikeItem {

    /**
     * The ID of the IOC.
     */
    @JsonProperty("id")
    private String id = null;

    /**
     * The type of the IOC.
     */
    @JsonProperty("type")
    private String type = null;

    /**
     * The value of the IOC.
     */
    @JsonProperty("indicator")
    private String indicator = null;

    /**
     * The epoch timestamp of the creation date of IOC.
     */
    @JsonProperty("published_date")
    private long publishedDate = 0L;


    /**
     * The epoch timestamp of the last updated date of IOC.
     */
    @JsonProperty("last_updated")
    private long lastUpdated = 0L;

}
