package com.desafio.obrigacoes.model;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum TipoObrigacao {

    DAS("Documento de Arrecadação do Simples Nacional",
            Periodicidade.MENSAL,
            EnumSet.of(RegimeTributario.SIMPLES)) {

        @Override
        public LocalDate calcularVencimento(String competencia) {
            return YearMonth.parse(competencia).plusMonths(1).atDay(20);
        }
    },

    DCTF("Declaração de Débitos e Créditos Tributários Federais",
            Periodicidade.MENSAL,
            EnumSet.of(RegimeTributario.LUCRO_PRESUMIDO, RegimeTributario.LUCRO_REAL)) {

        @Override
        public LocalDate calcularVencimento(String competencia) {
            return YearMonth.parse(competencia).plusMonths(2).atDay(15);
        }
    },

    ECD("Escrituração Contábil Digital",
            Periodicidade.ANUAL,
            EnumSet.allOf(RegimeTributario.class)) {

        @Override
        public LocalDate calcularVencimento(String competencia) {
            return Year.parse(competencia).plusYears(1).atMonth(Month.MAY).atDay(31);
        }
    };

    private final String descricao;
    private final Periodicidade periodicidade;
    private final Set<RegimeTributario> regimesAceitos;

    TipoObrigacao(String descricao, Periodicidade periodicidade, Set<RegimeTributario> regimesAceitos) {
        this.descricao = descricao;
        this.periodicidade = periodicidade;
        this.regimesAceitos = Collections.unmodifiableSet(regimesAceitos);
    }

    public abstract LocalDate calcularVencimento(String competencia);

    public boolean aceita(RegimeTributario regime) {
        return regimesAceitos.contains(regime);
    }

    public String competenciaPara(YearMonth mesReferencia) {
        return periodicidade == Periodicidade.MENSAL
                ? mesReferencia.toString()
                : String.valueOf(mesReferencia.getYear() - 1);
    }

    public String getDescricao() {
        return descricao;
    }

    public Periodicidade getPeriodicidade() {
        return periodicidade;
    }

    public Set<RegimeTributario> getRegimesAceitos() {
        return regimesAceitos;
    }
}
