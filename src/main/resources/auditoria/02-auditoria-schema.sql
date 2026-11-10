-- Schema auditoria (prefixo aud_) — separado de agendamentos da API

CREATE TABLE IF NOT EXISTS aud_clientes (
    id          BIGSERIAL PRIMARY KEY,
    nome        VARCHAR(120) NOT NULL,
    documento   VARCHAR(32)  NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS aud_motoristas (
    id          BIGSERIAL PRIMARY KEY,
    nome        VARCHAR(120) NOT NULL,
    cnh         VARCHAR(32)  NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS aud_entregas (
    id              BIGSERIAL PRIMARY KEY,
    cliente_id      BIGINT NOT NULL REFERENCES aud_clientes(id),
    motorista_id    BIGINT NOT NULL REFERENCES aud_motoristas(id),
    status          VARCHAR(40) NOT NULL,
    data_entrega    DATE NOT NULL,
    valor_centavos  INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS aud_ocorrencias (
    id              BIGSERIAL PRIMARY KEY,
    entrega_id      BIGINT NOT NULL REFERENCES aud_entregas(id),
    tipo            VARCHAR(80) NOT NULL,
    descricao       VARCHAR(255) NOT NULL,
    registrada_em   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS aud_reembolsos (
    id              BIGSERIAL PRIMARY KEY,
    entrega_id      BIGINT NOT NULL REFERENCES aud_entregas(id),
    status          VARCHAR(40) NOT NULL,
    motivo          VARCHAR(120) NOT NULL,
    valor_centavos  INTEGER NOT NULL,
    atualizado_em   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_aud_entregas_status ON aud_entregas(status);
CREATE INDEX IF NOT EXISTS idx_aud_entregas_data ON aud_entregas(data_entrega);
CREATE INDEX IF NOT EXISTS idx_aud_entregas_cliente ON aud_entregas(cliente_id);
CREATE INDEX IF NOT EXISTS idx_aud_entregas_motorista ON aud_entregas(motorista_id);
CREATE INDEX IF NOT EXISTS idx_aud_reembolsos_status ON aud_reembolsos(status);
CREATE INDEX IF NOT EXISTS idx_aud_reembolsos_entrega ON aud_reembolsos(entrega_id);
