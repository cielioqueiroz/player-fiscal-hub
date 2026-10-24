
ALTER TABLE empresa
    ADD COLUMN regime_tributario VARCHAR(30) NOT NULL DEFAULT 'SIMPLES';

ALTER TABLE empresa
    ALTER COLUMN regime_tributario DROP DEFAULT;

UPDATE empresa
   SET cnpj = REPLACE(REPLACE(REPLACE(cnpj, '.', ''), '/', ''), '-', '');

ALTER TABLE empresa
    MODIFY cnpj VARCHAR(14) NOT NULL;

ALTER TABLE empresa
    ADD CONSTRAINT uk_empresa_cnpj UNIQUE (cnpj);

CREATE TABLE empresa_obrigacao (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id  BIGINT      NOT NULL,
    tipo        VARCHAR(20) NOT NULL,
    criado_em   DATETIME    NOT NULL,

    CONSTRAINT fk_empresa_obrigacao_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa (id),

    CONSTRAINT uk_empresa_obrigacao
        UNIQUE (empresa_id, tipo)
);

CREATE TABLE entrega (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id           BIGINT       NOT NULL,
    tipo                 VARCHAR(20)  NOT NULL,
    competencia          VARCHAR(7)   NOT NULL,
    data_vencimento      DATE         NOT NULL,
    situacao             VARCHAR(20)  NOT NULL,
    data_conclusao       DATE         NULL,
    motivo_cancelamento  VARCHAR(500) NULL,
    versao               BIGINT       NOT NULL DEFAULT 0,
    criado_em            DATETIME     NOT NULL,

    CONSTRAINT fk_entrega_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa (id),

    CONSTRAINT uk_empresa_tipo_competencia
        UNIQUE (empresa_id, tipo, competencia)
);

CREATE TABLE documento (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    entrega_id     BIGINT       NOT NULL,
    nome_arquivo   VARCHAR(255) NOT NULL,
    content_type   VARCHAR(100) NOT NULL,
    tamanho_bytes  BIGINT       NOT NULL,
    conteudo       LONGBLOB     NOT NULL,
    enviado_em     DATETIME     NOT NULL,

    CONSTRAINT fk_documento_entrega
        FOREIGN KEY (entrega_id) REFERENCES entrega (id)
);
