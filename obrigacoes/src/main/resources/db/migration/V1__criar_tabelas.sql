CREATE TABLE empresa (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    cnpj VARCHAR(20) NOT NULL
);

CREATE TABLE obrigacao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    descricao VARCHAR(255) NOT NULL,
    tipo VARCHAR(100) NOT NULL,
    data_vencimento DATE NOT NULL,
    situacao VARCHAR(50) NOT NULL,
    empresa_id BIGINT NOT NULL,

    CONSTRAINT fk_obrigacao_empresa
        FOREIGN KEY (empresa_id)
        REFERENCES empresa(id)
);
