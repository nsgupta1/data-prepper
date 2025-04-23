package org.opensearch.dataprepper.plugins.source.crowdstrike;

import io.micrometer.core.instrument.Counter;
import org.opensearch.dataprepper.metrics.PluginMetrics;

import org.opensearch.dataprepper.plugins.source.crowdstrike.models.CrowdStrikeApiResponse;
import org.opensearch.dataprepper.plugins.source.crowdstrike.rest.CrowdStrikeRestClient;
import javax.inject.Named;

@Named
public class CrowdStrikeService {
    public static final String CONTENT_TYPE = "ContentType";
    private static final String SEARCH_RESULTS_FOUND = "searchResultsFound";

    private final CrowdStrikeSourceConfig crowdStrikeSourceConfig;
    private final CrowdStrikeRestClient crowdStrikeRestClient;
    private final Counter searchResultsFoundCounter;


    public CrowdStrikeService(CrowdStrikeSourceConfig crowdStrikeSourceConfig,
                              CrowdStrikeRestClient crowdStrikeRestClient,
                              PluginMetrics pluginMetrics) {
        this.crowdStrikeSourceConfig = crowdStrikeSourceConfig;
        this.crowdStrikeRestClient = crowdStrikeRestClient;
        this.searchResultsFoundCounter = pluginMetrics.counter(SEARCH_RESULTS_FOUND);
    }

    public CrowdStrikeApiResponse getAllContent(Long startTime, Long endTime, String paginationLink, String bearerToken) {
        return crowdStrikeRestClient.getAllContent(startTime, endTime, paginationLink, bearerToken);
    }
}
