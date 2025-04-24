package org.opensearch.dataprepper.plugins.source.crowdstrike.rest;

import com.google.common.annotations.VisibleForTesting;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.opensearch.dataprepper.metrics.PluginMetrics;
import org.opensearch.dataprepper.plugins.source.crowdstrike.models.CrowdStrikeApiResponse;
import org.opensearch.dataprepper.plugins.source.crowdstrike.models.CrowdStrikeIndicatorResult;
import org.opensearch.dataprepper.plugins.source.source_crawler.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.inject.Named;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.opensearch.dataprepper.logging.DataPrepperMarkers.NOISY;
import static org.opensearch.dataprepper.plugins.source.crowdstrike.utils.Constants.MAX_RETRIES;

@Named
public class CrowdStrikeRestClient {

    private static final List<Integer> RETRY_ATTEMPT_SLEEP_TIME = List.of(1, 2, 5, 10, 20, 40);
    static final String AUTH_FAILURES_COUNTER = "authFailures";
    private int sleepTimeMultiplier = 1000;
    private final Timer searchCallLatencyTimer;
    private static final String BASE_URL = "https://api.crowdstrike.com/";
    private static final String COMBINED_URL = "https://api.crowdstrike.com/intel/combined/indicators/v1";
    private static final Logger log = LoggerFactory.getLogger(CrowdStrikeRestClient.class);
    private final Counter authFailures;
    private final RestTemplate restTemplate;
    private final CrowdStrikeAuthClient authClient;

    public CrowdStrikeRestClient(PluginMetrics pluginMetrics, CrowdStrikeAuthClient authClient) {
        this.authFailures = pluginMetrics.counter(AUTH_FAILURES_COUNTER);
        this.searchCallLatencyTimer = pluginMetrics.timer("searchCallLatencyTimer");
        this.restTemplate = new RestTemplate();
        this.authClient = authClient;
    }

    /**
     * Method to get all Contents in a paginated fashion.
     *
     * @return InputStream input stream
     */
    public CrowdStrikeApiResponse getAllContent(Long startTime, Long endTime, String paginationLink) {
        URI uri;
        if (null != paginationLink) {
            try {
                String urlString = BASE_URL + paginationLink;
                uri = new URI(urlString);
            } catch (URISyntaxException e) {
                throw new RuntimeException("Failed to construct pagination url.", e);
            }
        } else {
            String fql1 = COMBINED_URL + String.format("?filter=last_updated:>=%d+last_updated:<%d&limit=10000", startTime, endTime);
            String encodedFql = UriComponentsBuilder.fromHttpUrl(fql1)
                    .encode()
                    .toUriString();
            String encodedFql1 = encodedFql.replace("+", "%2B");
            try {
                uri = new URI(encodedFql1);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return searchCallLatencyTimer.record(
                () -> {
                    try {
                        ResponseEntity<CrowdStrikeIndicatorResult> responseEntity = invokeGetApi(uri, CrowdStrikeIndicatorResult.class);
                        CrowdStrikeApiResponse response = new CrowdStrikeApiResponse();
                        response.setBody(responseEntity.getBody());
                        response.setHeaders(responseEntity.getHeaders());
                        return response;
                    } catch (Exception e) {
                        log.error("Error while fetching content with fql");
                        throw e;
                    }
                }
        );
    }

    public <T> ResponseEntity<T> invokeGetApi(URI uri, Class<T> responseType) {
        // Create headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authClient.getBearerToken());
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "application/json");

        // Create HTTP entity with headers
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);
        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                return restTemplate.exchange(uri, HttpMethod.GET, requestEntity, responseType);
            } catch (HttpClientErrorException ex) {
                HttpStatus statusCode = ex.getStatusCode();
                String statusMessage = ex.getMessage();
                log.error("An exception has occurred while getting response from search API  {}", ex.getMessage());
                if (statusCode == HttpStatus.FORBIDDEN) {
                    throw new UnauthorizedException(statusMessage);
                } else if (statusCode == HttpStatus.UNAUTHORIZED) {
                    authFailures.increment();
                    log.error(NOISY, "Token expired. We will try to renew the tokens now", ex);
                    authClient.refreshToken();
                } else if (statusCode == HttpStatus.TOO_MANY_REQUESTS) {
                    log.error(NOISY, "Hitting API rate limit. Backing off with sleep timer.", ex);
                }
                try {
                    Thread.sleep((long) RETRY_ATTEMPT_SLEEP_TIME.get(retryCount) * sleepTimeMultiplier);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Sleep in the retry attempt got interrupted", e);
                }
            }
            retryCount++;
        }
        String errorMessage = String.format("Exceeded max retry attempts. Failed to execute the Rest API call %s", uri);
        log.error(errorMessage);
        throw new RuntimeException(errorMessage);
    }



    @VisibleForTesting
    public void setSleepTimeMultiplier(int multiplier) {
        sleepTimeMultiplier = multiplier;
    }
}
