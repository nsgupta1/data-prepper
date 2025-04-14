/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 */

package org.opensearch.dataprepper.plugins.source.crowdstrike;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.Getter;
import org.opensearch.dataprepper.plugins.source.crowdstrike.configuration.Oauth2Config;
import org.opensearch.dataprepper.plugins.source.source_crawler.base.CrawlerSourceConfig;

@Getter
public class CrowdStrikeSourceConfig implements CrawlerSourceConfig {

    private static final int DEFAULT_BATCH_SIZE = 10000;

    /**
     * Batch size for fetching tickets
     */
    @JsonProperty("batch_size")
    protected int batchSize = DEFAULT_BATCH_SIZE;

    @JsonProperty("oauth2")
    @Valid
    private Oauth2Config oauth2Config;

    /**
     * Boolean property indicating end to end acknowledgments state
     */
    @JsonProperty("acknowledgments")
    private boolean acknowledgments = false;

}
