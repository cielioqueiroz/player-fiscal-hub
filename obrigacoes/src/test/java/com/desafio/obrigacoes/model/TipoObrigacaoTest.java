package com.desafio.obrigacoes.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TipoObrigacaoTest {

    @ParameterizedTest
    @CsvSource({
            "DAS,  2026-05, 2026-06-20",
            "DAS,  2026-12, 2027-01-20",
            "DAS,  2026-01, 2026-02-20",
            "DCTF, 2026-05, 2026-07-15",
            "DCTF, 2026-11, 2027-01-15",
            "DCTF, 2026-12, 2027-02-15",
            "ECD,  2025,    2026-05-31",
            "ECD,  2026,    2027-05-31"
    })
    void calculaVencimento(TipoObrigacao tipo, String competencia, LocalDate esperado) {
        assertThat(tipo.calcularVencimento(competencia)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @CsvSource({
            "DAS,  2026-05, 2026-05",
            "DCTF, 2026-05, 2026-05",
            "ECD,  2026-05, 2025",
            "ECD,  2026-01, 2025",
            "ECD,  2026-12, 2025"
    })
    void geraCompetenciaAPartirDoMesDeReferencia(TipoObrigacao tipo, YearMonth mesReferencia, String esperado) {
        assertThat(tipo.competenciaPara(mesReferencia)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @CsvSource({
            "DAS,  SIMPLES,         true",
            "DAS,  LUCRO_PRESUMIDO, false",
            "DAS,  LUCRO_REAL,      false",
            "DCTF, SIMPLES,         false",
            "DCTF, LUCRO_PRESUMIDO, true",
            "DCTF, LUCRO_REAL,      true",
            "ECD,  SIMPLES,         true",
            "ECD,  LUCRO_PRESUMIDO, true",
            "ECD,  LUCRO_REAL,      true"
    })
    void aceitaSomenteRegimesCompativeis(TipoObrigacao tipo, RegimeTributario regime, boolean esperado) {
        assertThat(tipo.aceita(regime)).isEqualTo(esperado);
    }

    @Test
    void ecdComReferenciaMaioDe2026VenceEm31DeMaioDe2026() {
        String competencia = TipoObrigacao.ECD.competenciaPara(YearMonth.of(2026, 5));

        assertThat(TipoObrigacao.ECD.calcularVencimento(competencia)).isEqualTo(LocalDate.of(2026, 5, 31));
    }

    @Test
    void regimesAceitosNaoPodemSerAlterados() {
        assertThatThrownBy(() -> TipoObrigacao.DAS.getRegimesAceitos().add(RegimeTributario.LUCRO_REAL))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "DAS,  2026",
            "DCTF, 2026-5",
            "ECD,  2026-05"
    })
    void competenciaEmFormatoErradoLancaExcecao(TipoObrigacao tipo, String competencia) {
        assertThatThrownBy(() -> tipo.calcularVencimento(competencia))
                .isInstanceOf(DateTimeParseException.class);
    }
}
