package org.opensearch.dataprepper.plugins.source.crowdstrike;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.opensearch.dataprepper.model.acknowledgements.AcknowledgementSet;
import org.opensearch.dataprepper.model.buffer.Buffer;
import org.opensearch.dataprepper.model.event.Event;
import org.opensearch.dataprepper.model.event.EventType;
import org.opensearch.dataprepper.model.event.JacksonEvent;
import org.opensearch.dataprepper.model.record.Record;
import org.opensearch.dataprepper.plugins.source.crowdstrike.models.CrowdStrikeApiResponse;
import org.opensearch.dataprepper.plugins.source.crowdstrike.models.CrowdStrikeIndicatorResult;
import org.opensearch.dataprepper.plugins.source.crowdstrike.models.ThreatIndicator;
import org.opensearch.dataprepper.plugins.source.crowdstrike.rest.CrowdStrikeAuthClient;
import org.opensearch.dataprepper.plugins.source.source_crawler.base.CrawlerClient;
import org.opensearch.dataprepper.plugins.source.source_crawler.base.PluginExecutorServiceProvider;
import org.opensearch.dataprepper.plugins.source.source_crawler.coordination.state.SaasWorkerProgressState;
import org.opensearch.dataprepper.plugins.source.source_crawler.model.ItemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import javax.inject.Named;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;


/**
 * This class represents a CrowdStrike client.
 */
@Named
public class CrowdStrikeClient implements CrawlerClient {
    CrowdStrikeService crowdStrikeService;
    private static final Logger log = LoggerFactory.getLogger(CrowdStrikeClient.class);
    private final ExecutorService executorService;
    private final CrowdStrikeSourceConfig configuration;
    private final CrowdStrikeAuthClient authClient;
    private final int bufferWriteTimeoutInSeconds = 10;
    private ObjectMapper objectMapper = new ObjectMapper();
    private Instant lastPollTime;


    public CrowdStrikeClient(CrowdStrikeService crowdStrikeService,
                             PluginExecutorServiceProvider executorServiceProvider,
                             CrowdStrikeSourceConfig sourceConfig,
                             CrowdStrikeAuthClient authClient) {
        log.info("Creating CrowdStrike Crawler");
        this.crowdStrikeService = crowdStrikeService;
        this.executorService = executorServiceProvider.get();
        this.configuration = sourceConfig;
        this.authClient = authClient;
        log.info("Created CrowdStrike Crawler");
    }

    @Override
    public Iterator<ItemInfo> listItems(Instant lastPollTime) {
        return null;
    }

    @Override
    public int getLookBackDays() {
        return configuration.getLookBackDays();
    }

    @Override
    public void executePartition(SaasWorkerProgressState state, Buffer<Record<Event>> buffer, AcknowledgementSet acknowledgementSet) {
        Long startTime = state.getExportStartTime().getEpochSecond();
        Long endTime = state.getExportStartTime().plus(Duration.ofMinutes(2)).getEpochSecond();
        StringBuilder fql = new StringBuilder()
                .append("last_updated:>=")
                .append(startTime)
                .append("+last_updated:<=")  // Using URL-safe separator
                .append(endTime);
        log.info("FQL query: {}", fql);
        String paginationLink = null;
        do {
            authClient.refreshToken();
            CrowdStrikeApiResponse crowdStrikeResponse = crowdStrikeService.getAllContent(startTime, endTime, paginationLink);
            CrowdStrikeIndicatorResult searchContentItems = crowdStrikeResponse.getBody();
            List<ThreatIndicator> contentList = new ArrayList<>(searchContentItems.getResults());
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
