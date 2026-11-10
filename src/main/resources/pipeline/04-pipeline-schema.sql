-- Schema pipeline batch (prefixo pipe_) — separado de aud_* e agendamentos

CREATE TABLE IF NOT EXISTS pipe_clientes_unidades (
    id              BIGSERIAL PRIMARY KEY,
    cliente_codigo  VARCHAR(40)  NOT NULL UNIQUE,
    cliente_nome    VARCHAR(120) NOT NULL,
    unidade         VARCHAR(80)  NOT NULL,
    regiao          VARCHAR(40)  NOT NULL
);

CREATE TABLE IF NOT EXISTS pipe_metricas_diarias (
    id                  BIGSERIAL PRIMARY KEY,
    data_ref            DATE         NOT NULL,
    regiao              VARCHAR(40)  NOT NULL,
    status              VARCHAR(40)  NOT NULL,
    cliente_codigo      VARCHAR(40)  NOT NULL,
    total_coletas       INTEGER      NOT NULL DEFAULT 0,
    total_entregas      INTEGER      NOT NULL DEFAULT 0,
    total_atrasos       INTEGER      NOT NULL DEFAULT 0,
    total_ocorrencias   INTEGER      NOT NULL DEFAULT 0,
    run_id              VARCHAR(64)  NOT NULL,
    UNIQUE (data_ref, regiao, status, cliente_codigo, run_id)
);

CREATE INDEX IF NOT EXISTS idx_pipe_metricas_data ON pipe_metricas_diarias(data_ref);
CREATE INDEX IF NOT EXISTS idx_pipe_metricas_regiao ON pipe_metricas_diarias(regiao);
