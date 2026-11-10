package com.mundodev.filafacil.pipeline.inter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class LateArrivingHandler {

    private final LatePolicy policy;

    public LateArrivingHandler(LatePolicy policy) {
        this.policy = policy;
    }

    public record LateResult(List<SourceRecord> accepted, long lateCount, long rejected) {
    }

    public LateResult apply(List<SourceRecord> records, LocalDateTime watermark) {
        List<SourceRecord> out = new ArrayList<>();
        long late = 0;
        long rejected = 0;
        LocalDateTime wm = watermark == null ? LocalDateTime.MIN : watermark;
        for (SourceRecord r : records) {
            boolean isLate = r.updatedAt().isBefore(wm);
            if (!isLate) {
                out.add(r);
                continue;
            }
            late++;
            r.setLateArriving(true);
            switch (policy) {
                case REJECT -> {
                    r.setLateAction("reject");
                    rejected++;
                }
                case ACCEPT -> {
                    r.setLateAction("accept");
                    out.add(r);
                }
                case MARK -> {
                    r.setLateAction("mark");
                    out.add(r);
                }
            }
        }
        return new LateResult(out, late, rejected);
    }
}
