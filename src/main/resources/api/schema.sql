CREATE TABLE IF NOT EXISTS agendamentos (
    id              BIGSERIAL PRIMARY KEY,
    transportadora  VARCHAR(120) NOT NULL,
    placa           VARCHAR(20)  NOT NULL,
    doca            VARCHAR(40)  NOT NULL,
    data_hora       TIMESTAMP    NOT NULL,
    status          VARCHAR(40)  NOT NULL,
    anexo_ref       VARCHAR(512),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
