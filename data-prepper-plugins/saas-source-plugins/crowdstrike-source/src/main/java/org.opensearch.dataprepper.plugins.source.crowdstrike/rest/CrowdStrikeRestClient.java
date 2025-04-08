package org.opensearch.dataprepper.plugins.source.crowdstrike.rest;

import com.google.common.annotations.VisibleForTesting;
import io.micrometer.core.instrument.Counter;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.dataprepper.metrics.PluginMetrics;
import org.opensearch.dataprepper.plugins.source.source_crawler.exception.BadRequestException;
import org.opensearch.dataprepper.plugins.source.source_crawler.exception.UnauthorizedException;
import org.opensearch.dataprepper.plugins.source.source_crawler.utils.AddressValidation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Named;
import java.net.URI;
import java.util.List;
import static org.opensearch.dataprepper.logging.DataPrepperMarkers.NOISY;

@Slf4j
@Named
public class CrowdStrikeRestClient {

        public static final List<Integer> RETRY_ATTEMPT_SLEEP_TIME = List.of(1, 2, 5, 10, 20, 40);
        static final String AUTH_FAILURES_COUNTER = "authFailures";
        private int sleepTimeMultiplier = 1000;
        final Counter authFailures;
        RestTemplate restTemplate = new RestTemplate();

        public CrowdStrikeRestClient(PluginMetrics pluginMetrics) {
            this.authFailures = pluginMetrics.counter(AUTH_FAILURES_COUNTER);
        }


        protected <T> ResponseEntity<T> invokeRestApi(URI uri, Class<T> responseType) throws BadRequestException {
            AddressValidation.validateInetAddress(AddressValidation.getInetAddress(uri.toString()));
            int retryCount = 0;
            while (retryCount < 5) {
                try {
                    return restTemplate.getForEntity(uri, responseType);
                } catch (HttpClientErrorException ex) {
                    HttpStatus statusCode = ex.getStatusCode();
                    String statusMessage = ex.getMessage();
                    log.error("An exception has occurred while getting response from search API  {}", ex.getMessage());
                    if (statusCode == HttpStatus.FORBIDDEN) {
                        throw new UnauthorizedException(statusMessage);
                    } else if (statusCode == HttpStatus.UNAUTHORIZED) {
                        authFailures.increment();
                        log.error(NOISY, "Token expired. We will try to renew the tokens now", ex);
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

