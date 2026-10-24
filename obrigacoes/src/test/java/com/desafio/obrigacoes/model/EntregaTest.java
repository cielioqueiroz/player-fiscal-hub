package com.desafio.obrigacoes.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.desafio.obrigacoes.exception.DadosInvalidosException;
import com.desafio.obrigacoes.exception.RegraNegocioException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class EntregaTest {

    private static final LocalDate VENCIMENTO = LocalDate.of(2026, 6, 20);
    private static final LocalDate HOJE = LocalDate.of(2026, 7, 1);
    private static final LocalDate CONCLUSAO = LocalDate.of(2026, 6, 18);

    private Entrega entrega() {
        return new Entrega(new Empresa(), TipoObrigacao.DAS, "2026-05", VENCIMENTO, LocalDateTime.of(2026, 5, 1, 0, 0));
    }

    private Entrega atrasada() {
        Entrega e = entrega();
        e.marcarAtrasada(HOJE);
        return e;
    }

    private Entrega concluida() {
        Entrega e = entrega();
        e.concluir(CONCLUSAO, HOJE);
        return e;
    }

    private Entrega cancelada() {
        Entrega e = entrega();
        e.cancelar("x");
        return e;
    }

    @Test
    void concluirAberta() {
        Entrega e = entrega();
        e.concluir(CONCLUSAO, HOJE);

        assertThat(e.getSituacao()).isEqualTo(SituacaoEntrega.CONCLUIDA);
        assertThat(e.getDataConclusao()).isEqualTo(CONCLUSAO);
    }

    @Test
    void concluirSemDataFalha() {
        assertThatThrownBy(() -> entrega().concluir(null, HOJE))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("dataConclusao é obrigatória para concluir a entrega");
    }

    @Test
    void concluirNoFuturoFalha() {
        assertThatThrownBy(() -> entrega().concluir(LocalDate.of(2026, 7, 2), HOJE))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("dataConclusao não pode estar no futuro");
    }

    @Test
    void concluirHojeEhPermitido() {
        Entrega e = entrega();
        e.concluir(HOJE, HOJE);

        assertThat(e.getSituacao()).isEqualTo(SituacaoEntrega.CONCLUIDA);
    }

    @Test
    void cancelarGuardaMotivoSemEspacos() {
        Entrega e = entrega();
        e.cancelar("  Encerrou  ");

        assertThat(e.getSituacao()).isEqualTo(SituacaoEntrega.CANCELADA);
        assertThat(e.getMotivoCancelamento()).isEqualTo("Encerrou");
    }

    @Test
    void cancelarSemMotivoFalha() {
        for (String motivo : new String[] {null, "", "   "}) {
            Entrega e = entrega();
            assertThatThrownBy(() -> e.cancelar(motivo))
                    .isInstanceOf(DadosInvalidosException.class)
                    .hasMessage("motivoCancelamento é obrigatório para cancelar a entrega");
            assertThat(e.getSituacao()).isEqualTo(SituacaoEntrega.ABERTA);
        }
    }

    @Test
    void cancelarComMotivoLongoFalha() {
        assertThatThrownBy(() -> entrega().cancelar("a".repeat(501)))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("motivoCancelamento deve ter no máximo 500 caracteres");
    }

    @Test
    void cancelarAceita500Caracteres() {
        Entrega e = entrega();
        e.cancelar("  " + "a".repeat(500) + "  ");

        assertThat(e.getSituacao()).isEqualTo(SituacaoEntrega.CANCELADA);
        assertThat(e.getMotivoCancelamento()).hasSize(500);
    }

    @Test
    void atrasadaNoDiaDoVencimentoFalha() {
        assertThatThrownBy(() -> entrega().marcarAtrasada(VENCIMENTO))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Só é possível marcar como ATRASADA após o vencimento (2026-06-20)");
    }

    @Test
    void atrasadaNoDiaSeguinte() {
        Entrega e = entrega();
        e.marcarAtrasada(LocalDate.of(2026, 6, 21));

        assertThat(e.getSituacao()).isEqualTo(SituacaoEntrega.ATRASADA);
    }

    @Test
    void atrasadaPodeSerConcluida() {
        Entrega e = atrasada();
        e.concluir(CONCLUSAO, HOJE);

        assertThat(e.getSituacao()).isEqualTo(SituacaoEntrega.CONCLUIDA);
    }

    @Test
    void atrasadaPodeSerCancelada() {
        Entrega e = atrasada();
        e.cancelar("Encerrou");

        assertThat(e.getSituacao()).isEqualTo(SituacaoEntrega.CANCELADA);
    }

    @Test
    void atrasadaNaoMarcaAtrasadaDeNovo() {
        assertThatThrownBy(() -> atrasada().marcarAtrasada(HOJE))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Transição de ATRASADA para ATRASADA não é permitida");
    }

    @Test
    void concluidaNaoMuda() {
        Entrega e = concluida();
        String mensagem = "Não é possível alterar uma entrega CONCLUIDA";

        assertThatThrownBy(() -> e.cancelar("x")).isInstanceOf(RegraNegocioException.class).hasMessage(mensagem);
        assertThatThrownBy(() -> e.concluir(CONCLUSAO, HOJE)).isInstanceOf(RegraNegocioException.class).hasMessage(mensagem);
        assertThatThrownBy(() -> e.marcarAtrasada(HOJE)).isInstanceOf(RegraNegocioException.class).hasMessage(mensagem);
    }

    @Test
    void canceladaNaoMuda() {
        Entrega e = cancelada();
        String mensagem = "Não é possível alterar uma entrega CANCELADA";

        assertThatThrownBy(() -> e.cancelar("x")).isInstanceOf(RegraNegocioException.class).hasMessage(mensagem);
        assertThatThrownBy(() -> e.concluir(CONCLUSAO, HOJE)).isInstanceOf(RegraNegocioException.class).hasMessage(mensagem);
        assertThatThrownBy(() -> e.marcarAtrasada(HOJE)).isInstanceOf(RegraNegocioException.class).hasMessage(mensagem);
    }

    @Test
    void concluidaCancelarSemMotivoEhRegraNaoDado() {
        assertThatThrownBy(() -> concluida().cancelar(null))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void falhaNaoAlteraConcluida() {
        Entrega e = concluida();

        assertThatThrownBy(() -> e.cancelar("x")).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> e.concluir(HOJE, HOJE)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> e.marcarAtrasada(HOJE)).isInstanceOf(RegraNegocioException.class);

        assertThat(e.getSituacao()).isEqualTo(SituacaoEntrega.CONCLUIDA);
        assertThat(e.getDataConclusao()).isEqualTo(CONCLUSAO);
        assertThat(e.getMotivoCancelamento()).isNull();
    }
}
