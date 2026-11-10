package com.mundodev.filafacil.pipeline.inter;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SourceRecord {

    private final SourceId source;
    private final String entity;
    private final String businessKey;
    private final LocalDateTime updatedAt;
    private final int version;
    private final boolean deleted;
    private final String regiao;
    private final String dt;
    private final Map<String, Object> payload;
    private CdcOp cdcOp = CdcOp.INSERT;
    private boolean lateArriving;
    private String lateAction;

    public SourceRecord(
            SourceId source,
            String entity,
            String businessKey,
            LocalDateTime updatedAt,
            int version,
            boolean deleted,
            String regiao,
            String dt,
            Map<String, Object> payload) {
        this.source = source;
        this.entity = entity;
        this.businessKey = businessKey;
        this.updatedAt = updatedAt;
        this.version = version;
        this.deleted = deleted;
        this.regiao = regiao == null ? "DESCONHECIDA" : regiao;
        this.dt = dt;
        this.payload = new LinkedHashMap<>(payload);
    }

    public SourceId source() {
        return source;
    }

    public String entity() {
        return entity;
    }

    public String businessKey() {
        return businessKey;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    public int version() {
        return version;
    }

    public boolean deleted() {
        return deleted;
    }

    public String regiao() {
        return regiao;
    }

    public String dt() {
        return dt;
    }

    public Map<String, Object> payload() {
        return payload;
    }

    public CdcOp cdcOp() {
        return cdcOp;
    }

    public void setCdcOp(CdcOp cdcOp) {
        this.cdcOp = cdcOp;
    }

    public boolean lateArriving() {
        return lateArriving;
    }

    public void setLateArriving(boolean lateArriving) {
        this.lateArriving = lateArriving;
    }

    public String lateAction() {
        return lateAction;
    }

    public void setLateAction(String lateAction) {
        this.lateAction = lateAction;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>(payload);
        m.put("source", source.id());
        m.put("entity", entity);
        m.put("business_key", businessKey);
        m.put("updated_at", updatedAt.toString());
        m.put("version", version);
        m.put("deleted", deleted);
        m.put("regiao", regiao);
        m.put("dt", dt);
        m.put("cdc_op", cdcOp.name());
        m.put("late_arriving", lateArriving);
        m.put("ativo", !deleted);
        return m;
    }
}
