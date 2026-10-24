package com.desafio.obrigacoes.migracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.desafio.obrigacoes.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class MigracaoV2IT {

    @Autowired
    JdbcTemplate jdbc;

    private long inserirEmpresa(String cnpj) {
        jdbc.update("INSERT INTO empresa (nome, cnpj, regime_tributario) VALUES ('Teste', ?, 'SIMPLES')", cnpj);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void inserirEntrega(long empresaId, String tipo, String competencia) {
        jdbc.update("""
                INSERT INTO entrega (empresa_id, tipo, competencia, data_vencimento, situacao, criado_em)
                VALUES (?, ?, ?, '2026-06-20', 'ABERTA', NOW())""", empresaId, tipo, competencia);
    }

    @Test
    void entregaDuplicadaEhRecusadaPelaUkEmpresaTipoCompetencia() {
        long empresa = inserirEmpresa("11222333000181");
        inserirEntrega(empresa, "DAS", "2026-05");

        assertThatThrownBy(() -> inserirEntrega(empresa, "DAS", "2026-05"))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("uk_empresa_tipo_competencia");
    }

    @Test
    void mesmaEmpresaETipoComCompetenciaDiferenteEhAceita() {
        long empresa = inserirEmpresa("11222333000181");
        inserirEntrega(empresa, "DAS", "2026-05");
        inserirEntrega(empresa, "DAS", "2026-06");
        inserirEntrega(empresa, "ECD", "2025");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM entrega WHERE empresa_id = ?", Integer.class, empresa))
                .isEqualTo(3);
    }

    @Test
    void mesmaEmpresaECompetenciaComTipoDiferenteEhAceita() {
        long empresa = inserirEmpresa("11222333000181");
        inserirEntrega(empresa, "DAS", "2026-05");
        inserirEntrega(empresa, "DCTF", "2026-05");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM entrega WHERE empresa_id = ?", Integer.class, empresa))
                .isEqualTo(2);
    }

    @Test
    void entregaNasceComVersaoZero() {
        long empresa = inserirEmpresa("11222333000181");
        inserirEntrega(empresa, "DAS", "2026-05");

        assertThat(jdbc.queryForObject("SELECT versao FROM entrega WHERE empresa_id = ?", Long.class, empresa))
                .isZero();
    }

    @Test
    void cnpjDuplicadoEhRecusado() {
        inserirEmpresa("11222333000181");

        assertThatThrownBy(() -> inserirEmpresa("11222333000181"))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("uk_empresa_cnpj");
    }

    @Test
    void empresaSemRegimeEhRecusada() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO empresa (nome, cnpj) VALUES ('Sem regime', '12345678000195')"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("regime_tributario");
    }

    @Test
    void vinculoDuplicadoEhRecusado() {
        long empresa = inserirEmpresa("11222333000181");
        String sql = "INSERT INTO empresa_obrigacao (empresa_id, tipo, criado_em) VALUES (?, 'DAS', NOW())";
        jdbc.update(sql, empresa);

        assertThatThrownBy(() -> jdbc.update(sql, empresa))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("uk_empresa_obrigacao");
    }

    @Test
    void mesmaEmpresaComTiposDiferentesDeVinculoEhAceita() {
        long empresa = inserirEmpresa("11222333000181");
        String sql = "INSERT INTO empresa_obrigacao (empresa_id, tipo, criado_em) VALUES (?, ?, NOW())";
        jdbc.update(sql, empresa, "DAS");
        jdbc.update(sql, empresa, "ECD");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM empresa_obrigacao WHERE empresa_id = ?", Integer.class, empresa))
                .isEqualTo(2);
    }

    @Test
    void entregaDeEmpresaInexistenteEhRecusada() {
        assertThatThrownBy(() -> inserirEntrega(999_999L, "DAS", "2026-05"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_entrega_empresa");
    }

    @Test
    void documentoDeEntregaInexistenteEhRecusado() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO documento (entrega_id, nome_arquivo, content_type, tamanho_bytes, conteudo, enviado_em)
                VALUES (999999, 'guia.pdf', 'application/pdf', 3, X'010203', NOW())"""))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_documento_entrega");
    }
}
