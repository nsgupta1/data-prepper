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

import io.micrometer.core.instrument.Counter;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.dataprepper.metrics.PluginMetrics;
import org.opensearch.dataprepper.plugins.source.crowdstrike.rest.CrowdStrikeRestClient;

import javax.inject.Named;



/**
 * Service class for interactive external Atlassian Confluence SaaS service and fetch required details using their rest apis.
 */

@Slf4j
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
}
