package com.mundodev.filafacil.pipeline.inter;

import java.util.Map;

public final class CdcClassifier {

    private CdcClassifier() {
    }

    public static CdcOp classify(SourceRecord incoming, Map<String, SourceRecord> consolidated) {
        if (incoming.deleted()) {
            return CdcOp.DELETE;
        }
        SourceRecord existing = consolidated.get(key(incoming));
        if (existing == null) {
            return CdcOp.INSERT;
        }
        int cmp = compareVersion(incoming, existing);
        if (cmp < 0) {
            return CdcOp.SKIP_STALE;
        }
        return CdcOp.UPDATE;
    }

    public static int compareVersion(SourceRecord a, SourceRecord b) {
        int ts = a.updatedAt().compareTo(b.updatedAt());
        if (ts != 0) {
            return ts;
        }
        int ver = Integer.compare(a.version(), b.version());
        if (ver != 0) {
            return ver;
        }
        return Integer.compare(a.source().priority(), b.source().priority());
    }

    public static String key(SourceRecord r) {
        return r.entity() + "|" + r.businessKey();
    }
}
