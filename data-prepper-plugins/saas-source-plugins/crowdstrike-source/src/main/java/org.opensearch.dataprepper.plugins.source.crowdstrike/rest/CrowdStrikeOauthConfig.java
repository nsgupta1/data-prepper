package org.opensearch.dataprepper.plugins.source.crowdstrike.rest;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.dataprepper.plugins.source.crowdstrike.CrowdStrikeSourceConfig;
import org.opensearch.dataprepper.plugins.source.crowdstrike.configuration.Oauth2Config;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Named;

import java.time.Instant;
import java.util.Map;

import static org.opensearch.dataprepper.logging.DataPrepperMarkers.NOISY;

@Slf4j
@Named
public class CrowdStrikeOauthConfig {


    private final CrowdStrikeSourceConfig sourceOauthConfig;
    RestTemplate restTemplate = new RestTemplate();

    // Getter for access token
    @Getter
    private String accessToken;
    @Getter
    private Instant expireTime;
    @Getter
    private int expiresInSeconds;
    private static final String OAUTH_TOKEN_URL = "https://api.crowdstrike.com/oauth2/token";

    public CrowdStrikeOauthConfig(CrowdStrikeSourceConfig sourceOauthConfig) {
        this.sourceOauthConfig = sourceOauthConfig;
    }

    public void initCredentials() {
        Oauth2Config oauth2Config = sourceOauthConfig.getOauth2Config();
        final String clientId = oauth2Config.getClientId();
        final String clientSecret = oauth2Config.getClientSecret();
        log.info(NOISY, "You are trying to access token");
        log.info(clientId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String payloadTemplate = "{ \"client_id\": \"%s\", \"client_secret\": \"%s\"}";
        String payload = String.format(payloadTemplate, clientId, clientSecret);
        HttpEntity<String> entity = new HttpEntity<>(payload, headers);
        try {
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(OAUTH_TOKEN_URL, entity, Map.class);
            Map<String, Object> oauthClientResponse = responseEntity.getBody();
            this.accessToken = (String) oauthClientResponse.get("access_token");
            this.expiresInSeconds = (int) oauthClientResponse.get("expires_in");
            this.expireTime = Instant.now().plusSeconds(expiresInSeconds);
            // updating config object's PluginConfigVariable so that it updates the underlying Secret store
            oauth2Config.getAccessToken().setValue(this.accessToken);
            log.info("Access Token and Refresh Token pair is now refreshed. Corresponding Secret store key updated.");
    } catch (HttpClientErrorException ex) {
        this.expireTime = Instant.ofEpochMilli(0);
        this.expiresInSeconds = 0;
        HttpStatus statusCode = ex.getStatusCode();
        log.error("Failed to renew access token. Status code: {}, Error Message: {}",
                statusCode, ex.getMessage());
        if (statusCode == HttpStatus.FORBIDDEN || statusCode == HttpStatus.UNAUTHORIZED) {
            log.info("Trying to refresh the secrets");
            // Refreshing the secrets. It should help if someone already renewed the tokens.
            // Refreshing one of the secret refreshes the entire store so triggering refresh on just one
            oauth2Config.getAccessToken().refresh();
            this.accessToken = (String) oauth2Config.getAccessToken().getValue();
            this.expireTime = Instant.now().plusSeconds(10);
        }
            throw new RuntimeException("Failed to renew access token message:" + ex.getMessage(), ex);
        }
    }

    public String getOauth2UrlContext() {
        if (accessToken == null) {
            throw new IllegalStateException("Access token not initialized. Call initCredentials() first.");
        }
        return "Bearer " + accessToken;
    }

}
