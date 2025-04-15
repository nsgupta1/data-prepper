package org.opensearch.dataprepper.plugins.source.crowdstrike;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.dataprepper.model.acknowledgements.AcknowledgementSet;
import org.opensearch.dataprepper.model.buffer.Buffer;
import org.opensearch.dataprepper.model.event.Event;
import org.opensearch.dataprepper.model.event.EventType;
import org.opensearch.dataprepper.model.event.JacksonEvent;
import org.opensearch.dataprepper.model.record.Record;
import org.opensearch.dataprepper.model.source.coordinator.enhanced.EnhancedSourceCoordinator;
import org.opensearch.dataprepper.plugins.source.crowdstrike.models.CrowdStrikeItem;
import org.opensearch.dataprepper.plugins.source.crowdstrike.models.CrowdStrikeResponse;
import org.opensearch.dataprepper.plugins.source.crowdstrike.models.CrowdStrikeSearchResults;
import org.opensearch.dataprepper.plugins.source.source_crawler.base.CrawlerClient;
import org.opensearch.dataprepper.plugins.source.source_crawler.base.CrawlerSourceConfig;
import org.opensearch.dataprepper.plugins.source.source_crawler.base.PluginExecutorServiceProvider;
import org.opensearch.dataprepper.plugins.source.source_crawler.coordination.partition.SaasSourcePartition;
import org.opensearch.dataprepper.plugins.source.source_crawler.coordination.state.SaasWorkerProgressState;
import org.opensearch.dataprepper.plugins.source.source_crawler.model.ItemInfo;
import org.springframework.util.CollectionUtils;

import javax.inject.Named;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
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


        // start = state.startTime()  --> Epoch based time
        // end = state.endTime()  --> Epoch based time
        Long startTime = state.getExportStartTime().getEpochSecond();
        Long endTime = state.getExportStartTime().plus(Duration.ofMinutes(5)).getEpochSecond();
        StringBuilder fql = new StringBuilder()
                .append("last_updated:>=")
                .append(startTime)
                .append("+last_updated:<")
                .append(endTime);

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


    @Override
    public void createPartition(Instant lastPollTime, List<ItemInfo> itemInfoList, EnhancedSourceCoordinator coordinator) {
        if (lastPollTime == Instant.EPOCH) {
            Instant initialDate = Instant.now();
            for (int i = 0; i < 90; i++) {
                SaasWorkerProgressState state = new SaasWorkerProgressState();
                state.setExportStartTime(initialDate.minus(Duration.ofDays(i)));
                SaasSourcePartition sourcePartition = new SaasSourcePartition(state, "last_updated"+"|"+  UUID.randomUUID());
                coordinator.createPartition(sourcePartition);
            }
        } else {
            SaasWorkerProgressState state = new SaasWorkerProgressState();
            state.setExportStartTime(lastPollTime);
            SaasSourcePartition sourcePartition = new SaasSourcePartition(state, "last_updated"+"|"+  UUID.randomUUID());
            coordinator.createPartition(sourcePartition);
        }
    }
}
