package org.elasticsearch.search.query.sortbydoc.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Weigher;
import com.google.common.collect.ImmutableMap;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.RamUsageEstimator;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * samuel
 * 22/10/15, 18:27
 */
public class ScoringDocumentCache extends AbstractComponent {
    private static final long BASE_RAM_BYTES_STRING = RamUsageEstimator.shallowSizeOfInstance(String.class) + RamUsageEstimator.NUM_BYTES_OBJECT_HEADER;
    private static final long BASE_RAM_BYTES_FLOAT = RamUsageEstimator.shallowSizeOfInstance(Float.class) + RamUsageEstimator.NUM_BYTES_OBJECT_HEADER;

    private static final ScoresEntry NO_SCORES = new ScoresEntry(0, ImmutableMap.of());

    private Client client;
    private final Cache<BytesRef, ScoresEntry> cache;

    @Inject
    public ScoringDocumentCache(Settings settings, Client client) {
        super(settings);
        this.client = client;

        ByteSizeValue size = settings.getAsBytesSize("size", new ByteSizeValue(10, ByteSizeUnit.MB));
        TimeValue expireAfterWrite = settings.getAsTime("expire_after_write", null);
        TimeValue expireAfterAccess = settings.getAsTime("expire_after_access", null);

        CacheBuilder<BytesRef, ScoresEntry> builder = CacheBuilder.newBuilder()
                .maximumWeight(size.bytes())
                .weigher(new TermsFilterValueWeigher());

        if (expireAfterAccess != null) {
            builder.expireAfterAccess(expireAfterAccess.millis(), TimeUnit.MILLISECONDS);
        }
        if (expireAfterWrite != null) {
            builder.expireAfterWrite(expireAfterWrite.millis(), TimeUnit.MILLISECONDS);
        }

        this.cache = builder.build();
    }

    private ScoresEntry getDocument(ScoresLookup lookup) {
        GetRequest request = new GetRequest(lookup.getIndex(), lookup.getType(), lookup.getId()).preference("_local").routing(lookup.getRouting());
        request.copyContextAndHeadersFrom(lookup.getHasContextAndHeaders());
        GetResponse getResponse = client.get(request).actionGet();
        if (!getResponse.isExists()) {
            return NO_SCORES;
        }

        Map<String, Float> scores = XContentGetScoreMap.extractMap(getResponse.getSourceAsMap(), lookup.getObjectPath(), lookup.getKeyField(), lookup.getValField());

        if (scores == null || scores.isEmpty()) {
            return NO_SCORES;
        }
        return new ScoresEntry(estimateSizeInBytes(scores), ImmutableMap.copyOf(scores));
    }

    public ImmutableMap<String, Float> getScores(final ScoresLookup lookup) {
        return getDocument(lookup).scores;
        /* Cache deactivated since we cannot clean it from the normal ES API because we are using a custom cache (to store a Map<> instead of only Terms)
        BytesRef key = new BytesRef(lookup.toString());

        try {
            return cache.get(key, new Callable<ScoresEntry>() {
                @Override
                public ScoresEntry call() throws Exception {
                    return getDocument(lookup);
                }
            }).scores;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new ElasticsearchException(e.getMessage(), e.getCause());
        }
        */
    }

    private long estimateSizeInBytes(Map<String, Float> scores) {
        long size = 8 + scores.size() * RamUsageEstimator.NUM_BYTES_OBJECT_REF;
        for (Map.Entry<String, Float> score : scores.entrySet()) {
                size += BASE_RAM_BYTES_STRING + (score.getKey().length() * RamUsageEstimator.NUM_BYTES_CHAR);
            size += BASE_RAM_BYTES_STRING + (score.getKey().length() * RamUsageEstimator.NUM_BYTES_CHAR);
            size += BASE_RAM_BYTES_FLOAT + 4;
        }
        return size;
    }

    static class TermsFilterValueWeigher implements Weigher<BytesRef, ScoresEntry> {

        @Override
        public int weigh(BytesRef key, ScoresEntry value) {
            return (int) (key.length + value.sizeInBytes);
        }
    }

    static class ScoresEntry {
        public final long sizeInBytes;
        public final ImmutableMap<String, Float> scores;

        ScoresEntry(long sizeInBytes, ImmutableMap scores) {
            this.sizeInBytes = sizeInBytes;
            this.scores = scores;
        }
    }
}
