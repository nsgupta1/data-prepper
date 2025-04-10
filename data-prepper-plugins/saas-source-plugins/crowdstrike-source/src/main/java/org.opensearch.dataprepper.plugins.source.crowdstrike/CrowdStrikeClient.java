package org.opensearch.dataprepper.plugins.source.crowdstrike;

import lombok.extern.slf4j.Slf4j;
import org.opensearch.dataprepper.model.acknowledgements.AcknowledgementSet;
import org.opensearch.dataprepper.model.buffer.Buffer;
import org.opensearch.dataprepper.model.event.Event;
import org.opensearch.dataprepper.model.record.Record;
import org.opensearch.dataprepper.plugins.source.source_crawler.base.CrawlerClient;
import org.opensearch.dataprepper.plugins.source.source_crawler.base.CrawlerSourceConfig;
import org.opensearch.dataprepper.plugins.source.source_crawler.base.PluginExecutorServiceProvider;
import org.opensearch.dataprepper.plugins.source.source_crawler.coordination.state.SaasWorkerProgressState;
import org.opensearch.dataprepper.plugins.source.source_crawler.model.ItemInfo;

import javax.inject.Named;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;

@Slf4j
@Named
public class CrowdStrikeClient implements CrawlerClient {

    CrowdStrikeService crowdStrikeService;
    CrowdStrikeIterator crowdStrikeIterator;
    private final ExecutorService executorService;
    private final CrawlerSourceConfig configuration;
    //private final int bufferWriteTimeoutInSeconds = 10;
    //private ObjectMapper objectMapper = new ObjectMapper();
    private Instant lastPollTime;

    public CrowdStrikeClient(CrowdStrikeService crowdStrikeService,
                             CrowdStrikeIterator crowdStrikeIterator,
                             PluginExecutorServiceProvider executorServiceProvider,
                             CrawlerSourceConfig sourceConfig) {
        log.info("Creating CrowdStrike Crawler");
        this.crowdStrikeService = crowdStrikeService;
        this.crowdStrikeIterator = crowdStrikeIterator;
        this.executorService = executorServiceProvider.get();
        this.configuration = sourceConfig;
        log.info("Created CrowdStrike Crawler");
    }

    @Override
    public Iterator<ItemInfo> listItems() {
        crowdStrikeIterator.initialize(lastPollTime);
        return crowdStrikeIterator;
    }

    @Override
    public void setLastPollTime(Instant lastPollTime) {
        log.trace("Setting the lastPollTime: {}", lastPollTime);
        this.lastPollTime = Instant.now().minus(Duration.ofMinutes(15));
    }

    @Override
    public void executePartition(SaasWorkerProgressState state, Buffer<Record<Event>> buffer, AcknowledgementSet acknowledgementSet) {
        log.trace("Executing the partition: {} with {} ticket(s)",
                state.getKeyAttributes(), state.getItemIds().size());


    }
}
