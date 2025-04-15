package org.opensearch.dataprepper.plugins.source.crowdstrike;

import lombok.Getter;
import lombok.Setter;
import org.opensearch.dataprepper.plugins.source.crowdstrike.models.CrowdStrikeItem;
import org.opensearch.dataprepper.plugins.source.source_crawler.model.ItemInfo;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class CrowdStrikeItemInfo implements ItemInfo {

    private String id;
    private String type;
    private Map<String, Object> metadata;
    private String indicator;
    private long publishedDate;
    private long lastUpdated;
    private Instant eventTime;
    private String maliciousConfidence;
    private String itemId;

    public CrowdStrikeItemInfo(String id, String itemId, String type, Map<String, Object> metadata, String indicator, long publishedDate, long lastUpdated, Instant eventTime, String maliciousConfidence) {
        this.id = id;
        this.type = type;
        this.metadata = metadata;
        this.indicator = indicator;
        this.publishedDate = publishedDate;
        this.lastUpdated = lastUpdated;
        this.eventTime = eventTime;
        this.maliciousConfidence = maliciousConfidence;
        this.itemId = itemId;
    }


    public static CrowdStrikeItemInfoBuilder builder() {
        return new CrowdStrikeItemInfoBuilder();
    }

    @Override
    public String getItemId() {
        return id;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public String getPartitionKey() {
        return lastUpdated + "|" + UUID.randomUUID();
    }


    @Override
    public Map<String, Object> getKeyAttributes() {
        return Map.of("type", type);
    }

    @Override
    public Instant getLastModifiedAt() {
        return getLastUpdated() != 0 ? Instant.ofEpochSecond(getLastUpdated()) : null;
    }

    public static class CrowdStrikeItemInfoBuilder {

        private String itemId;
        private String id;
        private String type;
        private Map<String, Object> metadata;
        private String indicator;
        private long publishedDate;
        private long lastUpdated;
        private Instant eventTime;
        private String maliciousConfidence;

        public CrowdStrikeItemInfoBuilder() {
        }

        public CrowdStrikeItemInfo build() {
            return new CrowdStrikeItemInfo(id, itemId, type, metadata, indicator, publishedDate, lastUpdated, eventTime, maliciousConfidence);
        }

        public CrowdStrikeItemInfoBuilder withItemId(String itemId) {
            this.itemId = itemId;
            return this;
        }

        public CrowdStrikeItemInfoBuilder withMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public CrowdStrikeItemInfoBuilder withId(String id) {
            this.id = id;
            return this;
        }

        public CrowdStrikeItemInfoBuilder withIndicator(String indicator) {
            this.indicator = indicator;
            return this;
        }

        public CrowdStrikeItemInfoBuilder withType(String type) {
            this.type = type;
            return this;
        }
        public CrowdStrikeItemInfoBuilder withLastUpdated(long lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public CrowdStrikeItemInfoBuilder withPublishedDate(long publishedDate) {
            this.publishedDate = publishedDate;
            return this;
        }

        public CrowdStrikeItemInfoBuilder withEventTime(Instant eventTime) {
            this.eventTime = eventTime;
            return this;
        }

        public CrowdStrikeItemInfoBuilder withMaliciousConfidence(String maliciousConfidence) {
            this.maliciousConfidence = maliciousConfidence;
            return this;
        }

        public CrowdStrikeItemInfoBuilder withContentBean(CrowdStrikeItem contentItem) {
            Map<String, Object> contentItemMetadata = new HashMap<>();
            contentItemMetadata.put("indicator", contentItem.getIndicator());
            contentItemMetadata.put("type", contentItem.getType());
            contentItemMetadata.put("malicious_confidence", contentItem.getMaliciousConfidence());
            contentItemMetadata.put("id", contentItem.getId());
            contentItemMetadata.put("published_date", contentItem.getPublishedDate());
            contentItemMetadata.put("last_updated", contentItem.getLastUpdated());

            this.id = contentItem.getId();
            this.type = contentItem.getType();
            this.itemId = contentItem.getId();
            this.metadata = contentItemMetadata;
            this.indicator = contentItem.getIndicator();
            this.publishedDate = contentItem.getPublishedDate();
            this.lastUpdated = contentItem.getLastUpdated();
            return this;
        }
    }
}
