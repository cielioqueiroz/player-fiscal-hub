package com.desafio.obrigacoes.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CnpjValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "11222333000181",
            "11.222.333/0001-81",
            "12345678000195",
            "11444777000161",
            "11222338000104",
            "11222335000170"
    })
    void aceitaCnpjsNumericosValidos(String cnpj) {
        assertThat(CnpjValidator.isValido(cnpj)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12ABC34501DE35", "12.ABC.345/01DE-35"})
    void aceitaCnpjsAlfanumericosValidos(String cnpj) {
        assertThat(CnpjValidator.isValido(cnpj)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"11-222-333-0001-81", "11222333/000181"})
    void aceitaSeparadoresEmQualquerPosicao(String cnpj) {
        assertThat(CnpjValidator.isValido(cnpj)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"  11.222.333/0001-81  ", "\t12.ABC.345/01DE-35\n", "12abc34501de35"})
    void aceitaEspacosNasPontasEMinusculas(String cnpj) {
        assertThat(CnpjValidator.isValido(cnpj)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"11222333000182", "11222333000191", "12345678000190", "12ABC34501DE36"})
    void rejeitaDigitoVerificadorErrado(String cnpj) {
        assertThat(CnpjValidator.isValido(cnpj)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"11111111111111", "00000000000000"})
    void rejeitaTodosOsCaracteresIguais(String cnpj) {
        assertThat(CnpjValidator.isValido(cnpj)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1122233300018", "112223330001811", "abc11222333000181xyz", "./-"})
    void rejeitaTamanhoErrado(String cnpj) {
        assertThat(CnpjValidator.isValido(cnpj)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"11.222.333/0001-8a", "12ABC34501DEA5"})
    void rejeitaLetraNosDigitosVerificadores(String cnpj) {
        assertThat(CnpjValidator.isValido(cnpj)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "11 222 333 0001 81",
            "11_222_333_0001_81",
            "\u00A011222333000181",
            "\uFF11\uFF11\uFF12\uFF12\uFF12\uFF13\uFF13\uFF13\uFF10\uFF10\uFF10\uFF11\uFF18\uFF11"
    })
    void rejeitaCaracteresForaDoPermitido(String cnpj) {
        assertThat(CnpjValidator.isValido(cnpj)).isFalse();
    }

    @Test
    void rejeitaEntradaLongaSemLancarExcecao() {
        assertThat(CnpjValidator.isValido("1".repeat(100))).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejeitaNuloEVazioSemLancarExcecao(String cnpj) {
        assertThat(CnpjValidator.isValido(cnpj)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "11.222.333/0001-81,         11222333000181",
            "11222333000181,             11222333000181",
            "'  11.222.333/0001-81  ',   11222333000181",
            "12.abc.345/01de-35,         12ABC34501DE35"
    })
    void normalizaSemMascaraEmMaiusculas(String entrada, String esperado) {
        assertThat(CnpjValidator.normalizar(entrada)).isEqualTo(esperado);
    }

    @Test
    void normalizarNuloDevolveNulo() {
        assertThat(CnpjValidator.normalizar(null)).isNull();
    }

    @Test
    void normalizarEntradaInvalidaLancaExcecao() {
        assertThatThrownBy(() -> CnpjValidator.normalizar("abc"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
