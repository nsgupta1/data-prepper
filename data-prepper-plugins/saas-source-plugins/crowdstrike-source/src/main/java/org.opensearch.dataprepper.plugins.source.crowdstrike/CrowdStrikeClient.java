package org.opensearch.dataprepper.plugins.source.crowdstrike;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.dataprepper.model.acknowledgements.AcknowledgementSet;
import org.opensearch.dataprepper.model.buffer.Buffer;
import org.opensearch.dataprepper.model.event.Event;
import org.opensearch.dataprepper.model.event.EventType;
import org.opensearch.dataprepper.model.event.JacksonEvent;
import org.opensearch.dataprepper.model.record.Record;
import org.opensearch.dataprepper.plugins.source.crowdstrike.models.CrowdStrikeItem;
import org.opensearch.dataprepper.plugins.source.crowdstrike.models.CrowdStrikeResponse;
import org.opensearch.dataprepper.plugins.source.crowdstrike.models.CrowdStrikeSearchResults;
import org.opensearch.dataprepper.plugins.source.source_crawler.base.CrawlerClient;
import org.opensearch.dataprepper.plugins.source.source_crawler.base.CrawlerSourceConfig;
import org.opensearch.dataprepper.plugins.source.source_crawler.base.PluginExecutorServiceProvider;
import org.opensearch.dataprepper.plugins.source.source_crawler.coordination.state.SaasWorkerProgressState;
import org.opensearch.dataprepper.plugins.source.source_crawler.model.ItemInfo;
import org.springframework.util.CollectionUtils;

import javax.inject.Named;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;


@Slf4j
@Named
public class CrowdStrikeClient implements CrawlerClient {

    CrowdStrikeService crowdStrikeService;
    CrowdStrikeIterator crowdStrikeIterator;
    private final ExecutorService executorService;
    private final CrawlerSourceConfig configuration;
    private final int bufferWriteTimeoutInSeconds = 10;
    private ObjectMapper objectMapper = new ObjectMapper();
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
        log.info("Executing the partition: {} with {} ticket(s)",
                state.getKeyAttributes(), state.getItemIds().size());

        // start = state.startTime()  --> Epoch based time
        // end = state.endTime()  --> Epoch based time
        Long startTime = Instant.now().minus(Duration.ofHours(24)).getEpochSecond();
        Long endTime = Instant.now().getEpochSecond();
        StringBuilder fql = new StringBuilder()
                .append("last_updated:>=")
                .append(startTime);

        String paginationLink = null;
        do {
            CrowdStrikeResponse crowdStrikeResponse = crowdStrikeService.getAllContent(fql, paginationLink);
            CrowdStrikeSearchResults searchContentItems = crowdStrikeResponse.getBody();
            List<CrowdStrikeItem> contentList = new ArrayList<>(searchContentItems.getResults());
            log.info(String.valueOf(contentList.size()));
            paginationLink = CollectionUtils.isEmpty(crowdStrikeResponse.getHeader("Next-Page")) ? null : crowdStrikeResponse.getHeader("Next-Page").get(0);
            List<Record<Event>> recordsToWrite = contentList
                    .parallelStream()
                    .map(t -> (Event) JacksonEvent.builder()
                            .withEventType(EventType.DOCUMENT.toString())
                            .withData(t)
                            .build())
                    .map(Record::new)
                    .collect(Collectors.toList());
            try {
                buffer.writeAll(recordsToWrite, (int) Duration.ofSeconds(bufferWriteTimeoutInSeconds).toMillis());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } while(paginationLink != null);

        if (configuration.isAcknowledgments()) {
            acknowledgementSet.complete();
        }
    }
}
