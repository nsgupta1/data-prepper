package org.opensearch.dataprepper.plugins.source.crowdstrike;

import lombok.extern.slf4j.Slf4j;
import org.opensearch.dataprepper.model.annotations.DataPrepperPluginConstructor;
import org.opensearch.dataprepper.plugins.source.source_crawler.base.PluginExecutorServiceProvider;
import org.opensearch.dataprepper.plugins.source.source_crawler.model.ItemInfo;

import javax.inject.Named;
import java.time.Instant;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

@Named
@Slf4j
public class CrowdStrikeIterator implements Iterator<ItemInfo> {

    private static final int HAS_NEXT_TIMEOUT = 60;
    private final CrowdStrikeService service;
    private final CrowdStrikeSourceConfig sourceConfig;
    private final ExecutorService crawlerTaskExecutor;
    private Queue<ItemInfo> itemInfoQueue;
    private Instant lastPollTime;
    private boolean firstTime = true;

    @DataPrepperPluginConstructor
    CrowdStrikeIterator(CrowdStrikeService service, CrowdStrikeSourceConfig sourceConfig, PluginExecutorServiceProvider executorServiceProvider) {
        this.service = service;
        this.sourceConfig = sourceConfig;
        this.crawlerTaskExecutor = executorServiceProvider.get();
    }

    /**
     * Initialize.
     *
     * @param crowdstrikeChangeLogToken the jira change log token
     */
    public void initialize(Instant crowdstrikeChangeLogToken) {
        this.itemInfoQueue = new ConcurrentLinkedQueue<>();
        this.lastPollTime = crowdstrikeChangeLogToken;
        this.firstTime = true;
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public ItemInfo next() {
        return null;
    }
}
