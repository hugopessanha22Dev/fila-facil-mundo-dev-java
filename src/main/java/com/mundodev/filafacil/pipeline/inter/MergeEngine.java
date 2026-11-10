package com.mundodev.filafacil.pipeline.inter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MergeEngine {

    public record MergeStats(long inserted, long updated, long deleted, long deduplicated, long skippedStale) {
    }

    private final Map<String, SourceRecord> state = new LinkedHashMap<>();
    private long inserted;
    private long updated;
    private long deleted;
    private long deduplicated;
    private long skippedStale;

    public MergeEngine() {
    }

    public MergeEngine(Map<String, SourceRecord> initial) {
        state.putAll(initial);
    }

    public void seed(SourceRecord existing) {
        state.put(CdcClassifier.key(existing), existing);
    }

    public void apply(SourceRecord incoming) {
        String key = CdcClassifier.key(incoming);
        SourceRecord current = state.get(key);
        CdcOp op = CdcClassifier.classify(incoming, state);
        incoming.setCdcOp(op);

        if (op == CdcOp.SKIP_STALE) {
            skippedStale++;
            deduplicated++;
            return;
        }
        if (current != null && op != CdcOp.INSERT) {
            deduplicated++;
        }
        switch (op) {
            case INSERT -> {
                state.put(key, incoming);
                inserted++;
            }
            case UPDATE -> {
                state.put(key, incoming);
                updated++;
            }
            case DELETE -> {
                state.put(key, incoming);
                deleted++;
            }
            default -> {
            }
        }
    }

    public void applyAll(List<SourceRecord> records) {
        // sort so newer/higher priority wins deterministically when applied in batch
        List<SourceRecord> sorted = new ArrayList<>(records);
        sorted.sort((a, b) -> {
            int c = a.updatedAt().compareTo(b.updatedAt());
            if (c != 0) {
                return c;
            }
            c = Integer.compare(a.version(), b.version());
            if (c != 0) {
                return c;
            }
            return Integer.compare(a.source().priority(), b.source().priority());
        });
        for (SourceRecord r : sorted) {
            apply(r);
        }
    }

    public Map<String, SourceRecord> state() {
        return state;
    }

    public List<SourceRecord> values() {
        return new ArrayList<>(state.values());
    }

    public MergeStats stats() {
        return new MergeStats(inserted, updated, deleted, deduplicated, skippedStale);
    }
}
