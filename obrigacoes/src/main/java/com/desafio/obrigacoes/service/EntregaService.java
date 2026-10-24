package com.desafio.obrigacoes.service;

import com.desafio.obrigacoes.dto.AlterarSituacaoRequest;
import com.desafio.obrigacoes.dto.EntregaResponse;
import com.desafio.obrigacoes.dto.ResumoEntregasResponse;
import com.desafio.obrigacoes.exception.DadosInvalidosException;
import com.desafio.obrigacoes.exception.RecursoNaoEncontradoException;
import com.desafio.obrigacoes.model.Entrega;
import com.desafio.obrigacoes.model.SituacaoEntrega;
import com.desafio.obrigacoes.model.TipoObrigacao;
import com.desafio.obrigacoes.repository.EntregaRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class EntregaService {

    private final EntregaRepository entregaRepository;
    private final Clock clock;

    public EntregaService(EntregaRepository entregaRepository, Clock clock) {
        this.entregaRepository = entregaRepository;
        this.clock = clock;
    }

    public Page<Entrega> listar(Long empresaId, TipoObrigacao tipo, String competencia, SituacaoEntrega situacao,
                                Pageable pageable) {
        return entregaRepository.buscar(empresaId, tipo, competencia, situacao, pageable);
    }

    public ResumoEntregasResponse resumir(Long empresaId) {
        LocalDate hoje = LocalDate.now(clock);
        return entregaRepository.resumir(empresaId, hoje, hoje.plusDays(7), SituacaoEntrega.ABERTA,
                SituacaoEntrega.ATRASADA, SituacaoEntrega.CONCLUIDA, SituacaoEntrega.CANCELADA);
    }

    public Entrega buscarPorId(Long id) {
        return entregaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entrega " + id + " não encontrada"));
    }

    @Transactional
    public EntregaResponse alterarSituacao(Long id, AlterarSituacaoRequest request) {
        if (request == null || request.situacao() == null) {
            throw new DadosInvalidosException("situacao é obrigatória");
        }

        Entrega entrega = buscarPorId(id);
        LocalDate hoje = LocalDate.now(clock);

        switch (request.situacao()) {
            case CONCLUIDA -> entrega.concluir(request.dataConclusao(), hoje);
            case CANCELADA -> entrega.cancelar(request.motivoCancelamento());
            case ATRASADA -> entrega.marcarAtrasada(hoje);
            case ABERTA -> entrega.garantirTransicaoPara(SituacaoEntrega.ABERTA);
        }

        return EntregaResponse.de(entrega, hoje);
    }
}
