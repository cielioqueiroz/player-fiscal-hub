package com.desafio.obrigacoes.model;

import com.desafio.obrigacoes.exception.DadosInvalidosException;
import com.desafio.obrigacoes.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "entrega")
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoObrigacao tipo;

    @Column(nullable = false, length = 7)
    private String competencia;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoEntrega situacao;

    @Column(name = "data_conclusao")
    private LocalDate dataConclusao;

    @Column(name = "motivo_cancelamento", length = 500)
    private String motivoCancelamento;

    @Version
    @Column(nullable = false)
    private Long versao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected Entrega() {
    }

    public Entrega(Empresa empresa, TipoObrigacao tipo, String competencia,
                   LocalDate dataVencimento, LocalDateTime criadoEm) {
        this.empresa = empresa;
        this.tipo = tipo;
        this.competencia = competencia;
        this.dataVencimento = dataVencimento;
        this.criadoEm = criadoEm;
        this.situacao = SituacaoEntrega.ABERTA;
    }

    public void concluir(LocalDate dataConclusao, LocalDate hoje) {
        garantirTransicaoPara(SituacaoEntrega.CONCLUIDA);

        if (dataConclusao == null) {
            throw new DadosInvalidosException("dataConclusao é obrigatória para concluir a entrega");
        }
        if (dataConclusao.isAfter(hoje)) {
            throw new DadosInvalidosException("dataConclusao não pode estar no futuro");
        }

        this.situacao = SituacaoEntrega.CONCLUIDA;
        this.dataConclusao = dataConclusao;
    }

    public void cancelar(String motivo) {
        garantirTransicaoPara(SituacaoEntrega.CANCELADA);

        if (motivo == null || motivo.isBlank()) {
            throw new DadosInvalidosException("motivoCancelamento é obrigatório para cancelar a entrega");
        }
        if (motivo.trim().length() > 500) {
            throw new DadosInvalidosException("motivoCancelamento deve ter no máximo 500 caracteres");
        }

        this.situacao = SituacaoEntrega.CANCELADA;
        this.motivoCancelamento = motivo.trim();
    }

    public void marcarAtrasada(LocalDate hoje) {
        garantirTransicaoPara(SituacaoEntrega.ATRASADA);

        if (!hoje.isAfter(dataVencimento)) {
            throw new RegraNegocioException(
                    "Só é possível marcar como ATRASADA após o vencimento (" + dataVencimento + ")");
        }

        this.situacao = SituacaoEntrega.ATRASADA;
    }

    public void garantirTransicaoPara(SituacaoEntrega destino) {
        if (situacao.isFinal()) {
            throw new RegraNegocioException("Não é possível alterar uma entrega " + situacao);
        }
        if (!situacao.podeIrPara(destino)) {
            throw new RegraNegocioException("Transição de " + situacao + " para " + destino + " não é permitida");
        }
    }

    public boolean isVencida(LocalDate hoje) {
        return !situacao.isFinal() && hoje.isAfter(dataVencimento);
    }

    public Long getId() {
        return id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public TipoObrigacao getTipo() {
        return tipo;
    }

    public String getCompetencia() {
        return competencia;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public SituacaoEntrega getSituacao() {
        return situacao;
    }

    public LocalDate getDataConclusao() {
        return dataConclusao;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public Long getVersao() {
        return versao;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
