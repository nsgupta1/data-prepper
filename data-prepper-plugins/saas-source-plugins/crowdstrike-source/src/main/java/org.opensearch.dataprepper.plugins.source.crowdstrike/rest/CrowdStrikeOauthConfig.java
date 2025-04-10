package org.opensearch.dataprepper.plugins.source.crowdstrike.rest;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.dataprepper.plugins.source.crowdstrike.CrowdStrikeSourceConfig;
import org.opensearch.dataprepper.plugins.source.crowdstrike.configuration.Oauth2Config;
import javax.inject.Named;

import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;


import java.time.Instant;
import java.util.Map;
import static org.opensearch.dataprepper.logging.DataPrepperMarkers.NOISY;

@Slf4j
@Named
public class CrowdStrikeOauthConfig {

    private final CrowdStrikeSourceConfig sourceOauthConfig;

    // Getter for access token
    @Getter
    private String accessToken;
    @Getter
    private Instant expireTime;
    @Getter
    private int expiresInSeconds;
    private static final String OAUTH_TOKEN_URL = "https://api.crowdstrike.com/oauth2/token";
    private final WebClient webClient;

    public CrowdStrikeOauthConfig(CrowdStrikeSourceConfig sourceOauthConfig) {
        this.sourceOauthConfig = sourceOauthConfig;
        this.webClient = WebClient.builder().build();
    }

    public void initCredentials() {
        Oauth2Config oauth2Config = this.sourceOauthConfig.getOauth2Config();
        final String clientId = oauth2Config.getClientId();
        final String clientSecret = oauth2Config.getClientSecret();
        log.info(NOISY, "You are trying to access token");
        log.info(clientId);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>(2) {{
            add("client_id", clientId);
            add("client_secret", clientSecret);
        }};

        Map<String, Object> response = webClient.post()
               .uri(CrowdStrikeOauthConfig.OAUTH_TOKEN_URL)
               .contentType(MediaType.APPLICATION_FORM_URLENCODED)
               .bodyValue(form)
               .retrieve()
               .bodyToMono(Map.class)
               .block();

        this.accessToken = response.get("access_token").toString();
        this.expiresInSeconds = (Integer) response.get("expires_in");
        this.expireTime = Instant.now().plusSeconds(expiresInSeconds);
        oauth2Config.setBearerToken(this.accessToken);
        log.info("access token acquired");
    }

    public String getOauth2UrlContext() {
        if (accessToken == null) {
            throw new IllegalStateException("Access token not initialized. Call initCredentials() first.");
        }
        return "Bearer " + accessToken;
    }

}
