-- Pipeline intermediario: checkpoints, lineage e CDC fields

ALTER TABLE pipe_clientes_unidades
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE pipe_clientes_unidades
    ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 1;

CREATE TABLE IF NOT EXISTS pipe_checkpoints (
    source_id       VARCHAR(40)  NOT NULL PRIMARY KEY,
    last_ts         TIMESTAMP    NOT NULL,
    interval_from   TIMESTAMP,
    interval_to     TIMESTAMP,
    batch_id        VARCHAR(64)  NOT NULL,
    records         INTEGER      NOT NULL DEFAULT 0,
    status          VARCHAR(40)  NOT NULL,
    run_id          VARCHAR(64)  NOT NULL,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS pipe_lineage (
    id              BIGSERIAL PRIMARY KEY,
    run_id          VARCHAR(64)  NOT NULL,
    source_id       VARCHAR(40)  NOT NULL,
    business_key    VARCHAR(80)  NOT NULL,
    transforms      VARCHAR(255) NOT NULL,
    dest            VARCHAR(120) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pipe_lineage_run ON pipe_lineage(run_id);
CREATE INDEX IF NOT EXISTS idx_pipe_lineage_key ON pipe_lineage(business_key);
