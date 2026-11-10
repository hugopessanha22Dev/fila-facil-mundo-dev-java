package com.mundodev.filafacil.pipeline.inter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public final class LineageStore {

    private final Path file;
    private final List<String> buffer = new ArrayList<>();

    public LineageStore(Path lineageDir, String runId) throws Exception {
        Files.createDirectories(lineageDir);
        this.file = lineageDir.resolve("lineage-" + runId + ".jsonl");
    }

    public void add(SourceRecord record, String transforms, String dest) {
        String line = "{\"run\":\"" + escape(record.source().id())
                + "\",\"source\":\"" + escape(record.source().id())
                + "\",\"business_key\":\"" + escape(record.businessKey())
                + "\",\"entity\":\"" + escape(record.entity())
                + "\",\"updated_at\":\"" + record.updatedAt()
                + "\",\"transforms\":\"" + escape(transforms)
                + "\",\"dest\":\"" + escape(dest)
                + "\",\"cdc_op\":\"" + record.cdcOp()
                + "\"}";
        // fix: include runId properly - use dest path's sibling naming; store run in transforms prefix
        buffer.add(line);
    }

    public void addWithRun(String runId, SourceRecord record, String transforms, String dest) {
        String line = "{\"run_id\":\"" + escape(runId)
                + "\",\"source\":\"" + escape(record.source().id())
                + "\",\"business_key\":\"" + escape(record.businessKey())
                + "\",\"entity\":\"" + escape(record.entity())
                + "\",\"updated_at\":\"" + record.updatedAt()
                + "\",\"transforms\":\"" + escape(transforms)
                + "\",\"dest\":\"" + escape(dest)
                + "\",\"cdc_op\":\"" + record.cdcOp()
                + "\"}";
        buffer.add(line);
    }

    public void flush() throws Exception {
        Files.write(file, buffer, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public Path file() {
        return file;
    }

    public List<String> buffer() {
        return List.copyOf(buffer);
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
