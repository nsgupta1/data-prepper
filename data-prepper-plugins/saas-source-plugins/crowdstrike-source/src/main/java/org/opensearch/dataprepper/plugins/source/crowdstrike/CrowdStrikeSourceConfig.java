package org.opensearch.dataprepper.plugins.source.crowdstrike;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import org.opensearch.dataprepper.plugins.source.crowdstrike.configuration.AuthenticationConfig;
import org.opensearch.dataprepper.plugins.source.source_crawler.base.CrawlerSourceConfig;
import org.opensearch.dataprepper.plugins.source.source_crawler.coordination.state.LeaderProgressState;

/**
 * Configuration class for the CrowdStrike source plugin.
 */
@Getter
public class CrowdStrikeSourceConfig implements CrawlerSourceConfig {

    private static final int DEFAULT_NUMBER_OF_WORKERS = 5;
    private static final int DEFAULT_LOOK_BACK_DAYS = 0;

    @JsonProperty("authentication")
    @Valid
    protected AuthenticationConfig authenticationConfig;

    @JsonProperty("acknowledgments")
    private boolean acknowledgments = false;

    @JsonProperty("look_back_days")
    @Min(0)
    @Max(50)
    @Valid
    private int lookBackDays = DEFAULT_LOOK_BACK_DAYS;

    @JsonProperty("workers")
    @Min(1)
    @Max(50)
    @Valid
    private int numberOfWorkers = DEFAULT_NUMBER_OF_WORKERS;

}
