package org.opensearch.dataprepper.plugins.source.crowdstrike.rest;

import com.google.common.annotations.VisibleForTesting;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.dataprepper.metrics.PluginMetrics;
import org.opensearch.dataprepper.plugins.source.crowdstrike.CrowdStrikeSourceConfig;
import org.opensearch.dataprepper.plugins.source.crowdstrike.configuration.Oauth2Config;
import org.opensearch.dataprepper.plugins.source.crowdstrike.models.CrowdStrikeSearchResults;
import org.opensearch.dataprepper.plugins.source.source_crawler.exception.UnauthorizedException;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.inject.Named;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

@Slf4j
@Named
public class CrowdStrikeRestClient {

        public static final List<Integer> RETRY_ATTEMPT_SLEEP_TIME = List.of(1, 2, 5, 10, 20, 40);
        static final String AUTH_FAILURES_COUNTER = "authFailures";
        private int sleepTimeMultiplier = 1000;
        private final Timer searchCallLatencyTimer;
        private final Oauth2Config authConfig;
        private final HttpClient httpClient;
        private static final String BASE_URL = "https://api.crowdstrike.com/";
    private static final String COMBINED_URL = "https://api.crowdstrike.com/intel/combined/indicators/v1";


    final Counter authFailures;
        RestTemplate restTemplate = new RestTemplate();

        public CrowdStrikeRestClient(PluginMetrics pluginMetrics, CrowdStrikeSourceConfig sourceOauthConfig) {
            this.authConfig = sourceOauthConfig.getOauth2Config();
            this.authFailures = pluginMetrics.counter(AUTH_FAILURES_COUNTER);
            this.searchCallLatencyTimer = pluginMetrics.timer("searchCallLatencyTimer");
            this.httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
        }

    /**
     * Method to get all Contents in a paginated fashion.
     *
     * @param fql     input parameter.
     * @return InputStream input stream
     */
    public CrowdStrikeSearchResults getAllContent(StringBuilder fql, String paginationLink) {

        URI uri;
        if (null != paginationLink) {
            try {
                String urlString =  BASE_URL + paginationLink;
                uri = new URI(urlString);
            } catch (URISyntaxException  e) {
                throw new RuntimeException("Failed to construct pagination url.", e);
            }
        } else {
            uri = UriComponentsBuilder.fromHttpUrl(COMBINED_URL)
                    .queryParam("limit", "100")
                    .buildAndExpand().toUri();
        }
        return searchCallLatencyTimer.record(
                () -> {
                    try {
                         ResponseEntity<CrowdStrikeSearchResults> responseEntity =  invokeGetApi(uri, CrowdStrikeSearchResults.class);
                         responseEntity.getHeaders().get("Next-Page");
                         return responseEntity.getBody();
                    } catch (Exception e) {
                        log.error("Error while fetching content with fql {}", fql);
                        //searchRequestsFailedCounter.increment();
                        throw e;
                    }
                }
        );
    }

    public <T> ResponseEntity<T> invokeGetApi(URI uri, Class<T> responseType) {
        // Create headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authConfig.getBearerToken());
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "application/json");

        // Create HTTP entity with headers
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        try {
            return restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    requestEntity,
                    responseType
            );
        } catch (HttpClientErrorException e) {
            handleHttpClientError(e);
            throw e;
        } catch (RestClientException e) {
            log.error("Error making REST call: ", e);
            throw new RuntimeException("Failed to make REST call", e);
        }
    }

    private void handleHttpClientError(HttpClientErrorException e) {
        switch (e.getStatusCode()) {
            case UNAUTHORIZED:
                log.error("Unauthorized access. Check bearer token");
                throw new UnauthorizedException("Invalid bearer token");
            case FORBIDDEN:
                log.error("Forbidden access to resource");
                throw new UnauthorizedException("Access denied");
            case NOT_FOUND:
                log.error("Resource not found");
                throw new UnauthorizedException("Resource not found");
            default:
                log.error("HTTP error occurred: {}", e.getStatusCode());
                throw e;
        }
    }

        @VisibleForTesting
        public void setSleepTimeMultiplier(int multiplier) {
            sleepTimeMultiplier = multiplier;
        }
}

