package org.opensearch.dataprepper.plugins.source.crowdstrike;
import io.micrometer.core.instrument.Timer;
import org.opensearch.dataprepper.metrics.PluginMetrics;
import org.opensearch.dataprepper.plugins.source.crowdstrike.models.CrowdStrikeApiResponse;
import org.opensearch.dataprepper.plugins.source.crowdstrike.models.CrowdStrikeIndicatorResult;
import org.opensearch.dataprepper.plugins.source.crowdstrike.rest.CrowdStrikeRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import javax.inject.Named;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Named
public class CrowdStrikeService {

    public static final String CONTENT_TYPE = "ContentType";
    private static final String SEARCH_RESULTS_FOUND = "searchResultsFound";
    private final CrowdStrikeRestClient crowdStrikeRestClient;
    private static final Logger log = LoggerFactory.getLogger(CrowdStrikeService.class);
    private static final String BASE_URL = "https://api.crowdstrike.com/";
    private static final String COMBINED_URL = "https://api.crowdstrike.com/intel/combined/indicators/v1";
    private final Timer searchCallLatencyTimer;


    public CrowdStrikeService(CrowdStrikeRestClient crowdStrikeRestClient, PluginMetrics pluginMetrics) {
        this.crowdStrikeRestClient = crowdStrikeRestClient;
        this.searchCallLatencyTimer = pluginMetrics.timer("searchCallLatencyTimer");
    }

    /**
     * Method to get all Contents in a paginated fashion.
     *
     * @return InputStream input stream
     */
    public CrowdStrikeApiResponse getAllContent(Long startTime, Long endTime, String paginationLink) {
        URI uri = buildCrowdStrikeUri(startTime, endTime, paginationLink);

        return searchCallLatencyTimer.record(() -> {
            try {
                log.info("Calling CrowdStrike API with URI: {}", uri);
                ResponseEntity<CrowdStrikeIndicatorResult> responseEntity = crowdStrikeRestClient.invokeGetApi(uri, CrowdStrikeIndicatorResult.class);

                CrowdStrikeApiResponse response = new CrowdStrikeApiResponse();
                response.setBody(responseEntity.getBody());
                response.setHeaders(responseEntity.getHeaders());
                return response;
            } catch (Exception e) {
                log.error("Error fetching CrowdStrike content from URI: {}", uri, e);
                throw new RuntimeException("CrowdStrike API call failed", e);
            }
        });
    }

    private URI buildCrowdStrikeUri(Long startTime, Long endTime, String paginationLink) {
        try {
            if (paginationLink != null) {
                return new URI(BASE_URL + paginationLink);
            } else {
                // Manually construct and encode the query string
                String filterValue = String.format("last_updated:>=%d+last_updated:<%d", startTime, endTime);
                String encodedFilter = URLEncoder.encode(filterValue, StandardCharsets.UTF_8)
                        .replace("+", "%2B"); // ensure literal '+'

                UriComponentsBuilder builder = UriComponentsBuilder
                        .fromHttpUrl(COMBINED_URL)
                        .queryParam("filter", encodedFilter)
                        .queryParam("limit", 10000);

                return builder.build(true).toUri();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct CrowdStrike request URI", e);
        }
    }
}
