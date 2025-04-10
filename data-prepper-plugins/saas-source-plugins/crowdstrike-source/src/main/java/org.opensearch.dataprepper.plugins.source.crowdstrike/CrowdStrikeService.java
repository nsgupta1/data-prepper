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
import org.opensearch.dataprepper.plugins.source.crowdstrike.models.CrowdStrikeItem;
import org.opensearch.dataprepper.plugins.source.crowdstrike.models.CrowdStrikeSearchResults;
import org.opensearch.dataprepper.plugins.source.crowdstrike.rest.CrowdStrikeRestClient;
import org.opensearch.dataprepper.plugins.source.source_crawler.model.ItemInfo;

import javax.inject.Named;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;


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



    /**
     * Get Confluence entities.
     *
     * @param configuration the configuration.
     * @param timestamp     timestamp.
     */
    public void getPages(CrowdStrikeSourceConfig configuration, Instant timestamp, Queue<ItemInfo> itemInfoQueue) {
        log.trace("Started to fetch entities");
        searchForNewContentAndAddToQueue(configuration, timestamp, itemInfoQueue);
        log.trace("Creating item information and adding in queue");
    }


    /**
     * Method for building Content Item Info.
     *
     * @param configuration Input Parameter
     * @param timestamp     Input Parameter
     */
    private void searchForNewContentAndAddToQueue(CrowdStrikeSourceConfig configuration, Instant timestamp,
                                                  Queue<ItemInfo> itemInfoQueue) {
        log.trace("Looking for Add/Modified tickets with a Search API call");
        StringBuilder fql = createContentFilterCriteria(configuration, timestamp);
        int total = 0;
        String paginationLink = null;
        do {
            CrowdStrikeSearchResults searchContentItems = crowdStrikeRestClient.getAllContent(fql, paginationLink);
            List<CrowdStrikeItem> contentList = new ArrayList<>(searchContentItems.getResults());
            total += searchContentItems.getTotal();
            log.info(String.valueOf(contentList.size()));
            //addItemsToQueue(contentList, itemInfoQueue);
            log.debug("Content items fetched so far: {}", total);
            paginationLink = searchContentItems.getLink();
        } while (paginationLink != null);
        log.info("Number of content items found in search api call: {}", total);
    }


    /**
     * Method for creating Content Filter Criteria.
     *
     * @param configuration Input Parameter
     * @param ts            Input Parameter
     * @return String Builder
     */
    private StringBuilder createContentFilterCriteria(CrowdStrikeSourceConfig configuration, Instant ts) {
        log.info("Creating Threat Intel filter criteria");
        StringBuilder fql = new StringBuilder("last_updated" + ">=" + ts.toEpochMilli());
        fql.append("sort" + "=" + "_marker" + "|asc");
        log.info("Created content filter criteria Falcon API query: {}", fql);
        return fql;
    }
}
