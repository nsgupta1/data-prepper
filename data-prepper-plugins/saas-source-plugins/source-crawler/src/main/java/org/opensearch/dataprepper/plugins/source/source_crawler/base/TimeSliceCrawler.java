package org.opensearch.dataprepper.plugins.source.source_crawler.base;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.opensearch.dataprepper.metrics.PluginMetrics;
import org.opensearch.dataprepper.model.acknowledgements.AcknowledgementSet;
import org.opensearch.dataprepper.model.buffer.Buffer;
import org.opensearch.dataprepper.model.event.Event;
import org.opensearch.dataprepper.model.record.Record;
import org.opensearch.dataprepper.model.source.coordinator.enhanced.EnhancedSourceCoordinator;
import org.opensearch.dataprepper.plugins.source.source_crawler.coordination.partition.LeaderPartition;
import org.opensearch.dataprepper.plugins.source.source_crawler.coordination.partition.SaasSourcePartition;
import org.opensearch.dataprepper.plugins.source.source_crawler.coordination.state.LeaderProgressState;
import org.opensearch.dataprepper.plugins.source.source_crawler.coordination.state.SaasWorkerProgressState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Named;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.opensearch.dataprepper.plugins.source.source_crawler.coordination.scheduler.LeaderScheduler.DEFAULT_EXTEND_LEASE_MINUTES;

@Named
public class TimeSliceCrawler implements Crawler {
    private static final Logger log = LoggerFactory.getLogger(TimeSliceCrawler.class);
    private static final int batchSize = 10000;
    private static final String TIME_SLICE_WORKER_PARTITIONS_CREATED = "TimeSliceWorkerPartitionsCreated";
    private static final String INVALID_PAGINATION_ITEMS = "invalidPaginationItems";
    private final Timer crawlingTimer;
    private final CrawlerClient client;
    private final Counter parititionsCreatedCounter;
    private final Counter invalidPaginationItemsCounter;

    public TimeSliceCrawler(CrawlerClient client, PluginMetrics pluginMetrics) {
        this.client = client;
        this.crawlingTimer = pluginMetrics.timer("crawlingTime");
        this.parititionsCreatedCounter = pluginMetrics.counter(TIME_SLICE_WORKER_PARTITIONS_CREATED);
        this.invalidPaginationItemsCounter = pluginMetrics.counter(INVALID_PAGINATION_ITEMS);
    }

    @Override
    public Instant crawl(LeaderPartition leaderPartition, EnhancedSourceCoordinator coordinator) {
        long startTime = System.currentTimeMillis();
        LeaderProgressState leaderProgressState = leaderPartition.getProgressState().get();
        // Leader state is always saved in UTC since the timestamps in the api response are always in UTC
        Instant lastPollTime = leaderProgressState.getLastPollTime();
        createPartition(lastPollTime,coordinator);
        Instant latestModifiedTime = lastPollTime;
        // Check point leader progress state at every minute interval.
        updateLeaderProgressState(leaderPartition, latestModifiedTime, coordinator);
        long crawlTimeMillis = System.currentTimeMillis() - startTime;
        log.debug("Crawling completed in {} ms", crawlTimeMillis);
        crawlingTimer.record(crawlTimeMillis, TimeUnit.MILLISECONDS);
        return latestModifiedTime;
    }

    private void updateLeaderProgressState(LeaderPartition leaderPartition, Instant updatedPollTime, EnhancedSourceCoordinator coordinator) {
        LeaderProgressState leaderProgressState = leaderPartition.getProgressState().get();
        leaderProgressState.setLastPollTime(updatedPollTime);
        leaderPartition.setLeaderProgressState(leaderProgressState);
        coordinator.saveProgressStateForPartition(leaderPartition, DEFAULT_EXTEND_LEASE_MINUTES);
    }

    public void createPartition(Instant lastPollTime, EnhancedSourceCoordinator coordinator) {
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

    @Override
    public void executePartition(SaasWorkerProgressState state, Buffer<Record<Event>> buffer, AcknowledgementSet acknowledgementSet) {
        client.executePartition(state, buffer, acknowledgementSet);
    }
}
