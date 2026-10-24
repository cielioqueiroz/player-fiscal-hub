package com.desafio.obrigacoes.migracao;

import static org.assertj.core.api.Assertions.assertThat;

import com.desafio.obrigacoes.TestcontainersConfiguration;
import com.desafio.obrigacoes.model.Empresa;
import com.desafio.obrigacoes.model.RegimeTributario;
import com.desafio.obrigacoes.repository.EmpresaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class EmpresaRegimeMapeamentoIT {

    @Autowired EmpresaRepository empresaRepository;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @Test
    void regimeEhGravadoComoTextoELidoDeVoltaComoEnum() {
        Empresa empresa = new Empresa();
        empresa.setNome("Gama Indústria");
        empresa.setCnpj("11444777000161");
        empresa.setRegimeTributario(RegimeTributario.LUCRO_REAL);

        Long id = empresaRepository.saveAndFlush(empresa).getId();
        entityManager.clear();

        assertThat(jdbc.queryForObject("SELECT regime_tributario FROM empresa WHERE id = ?", String.class, id))
                .isEqualTo("LUCRO_REAL");
        assertThat(empresaRepository.findById(id).orElseThrow().getRegimeTributario())
                .isEqualTo(RegimeTributario.LUCRO_REAL);
    }
}
