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


import org.opensearch.dataprepper.metrics.PluginMetrics;
import org.opensearch.dataprepper.model.acknowledgements.AcknowledgementSetManager;
import org.opensearch.dataprepper.model.annotations.DataPrepperPlugin;
import org.opensearch.dataprepper.model.annotations.DataPrepperPluginConstructor;
import org.opensearch.dataprepper.model.buffer.Buffer;
import org.opensearch.dataprepper.model.event.Event;
import org.opensearch.dataprepper.model.plugin.PluginFactory;
import org.opensearch.dataprepper.model.record.Record;
import org.opensearch.dataprepper.model.source.Source;
import org.opensearch.dataprepper.plugins.source.crowdstrike.utils.CrowdStrikeConfigHelper;
import org.opensearch.dataprepper.plugins.source.source_crawler.CrawlerApplicationContextMarker;
import org.opensearch.dataprepper.plugins.source.source_crawler.base.Crawler;
import org.opensearch.dataprepper.plugins.source.source_crawler.base.CrawlerSourcePlugin;
import org.opensearch.dataprepper.plugins.source.source_crawler.base.PluginExecutorServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opensearch.dataprepper.plugins.source.crowdstrike.rest.CrowdStrikeOauthConfig;


/**
 * CrowdStrike Source is the connector entry point.
 */
@DataPrepperPlugin(name = "crowdstrike",
        pluginType = Source.class,
        pluginConfigurationType = CrowdStrikeSourceConfig.class,
        packagesToScan = {CrawlerApplicationContextMarker.class, CrowdStrikeSource.class}
)
public class CrowdStrikeSource extends CrawlerSourcePlugin {

    private static final Logger log = LoggerFactory.getLogger(CrowdStrikeSource.class);
    private final CrowdStrikeSourceConfig crowdStrikeSourceConfig;
    private final CrowdStrikeOauthConfig crowdStrikeOauthConfig;

    @DataPrepperPluginConstructor
    public CrowdStrikeSource(final PluginMetrics pluginMetrics,
                            final CrowdStrikeSourceConfig crowdStrikeSourceConfig,
                            final CrowdStrikeOauthConfig crowdStrikeOauthConfig,
                             final PluginFactory pluginFactory,
                             final AcknowledgementSetManager acknowledgementSetManager,
                             Crawler crawler,
                             PluginExecutorServiceProvider executorServiceProvider) {
        super("crowdstrike", pluginMetrics, crowdStrikeSourceConfig, pluginFactory, acknowledgementSetManager, crawler, executorServiceProvider);
        log.info("Creating CrowdStrike Source Plugin");
        this.crowdStrikeSourceConfig = crowdStrikeSourceConfig;
        this.crowdStrikeOauthConfig = crowdStrikeOauthConfig;
    }

    @Override
    public void start(Buffer<Record<Event>> buffer) {
        log.info("Starting CrowdStrike Source Plugin... ");
        CrowdStrikeConfigHelper.validateConfig(crowdStrikeSourceConfig);
        crowdStrikeOauthConfig.initCredentials();
        super.start(buffer);
    }

    @Override
    public void stop() {
        log.info("Stopping CrowdStrike Source Plugin");
        super.stop();
    }

}