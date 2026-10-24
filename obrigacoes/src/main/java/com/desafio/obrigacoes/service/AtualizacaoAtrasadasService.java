package com.desafio.obrigacoes.service;

import com.desafio.obrigacoes.model.SituacaoEntrega;
import com.desafio.obrigacoes.repository.EntregaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class AtualizacaoAtrasadasService {

    private static final Logger log = LoggerFactory.getLogger(AtualizacaoAtrasadasService.class);

    private final EntregaRepository entregaRepository;
    private final Clock clock;

    public AtualizacaoAtrasadasService(EntregaRepository entregaRepository, Clock clock) {
        this.entregaRepository = entregaRepository;
        this.clock = clock;
    }

    @Transactional
    public int atualizarAtrasadas() {
        int atualizadas = entregaRepository.atualizarAbertasVencidas(
                LocalDate.now(clock), SituacaoEntrega.ABERTA, SituacaoEntrega.ATRASADA);
        log.info("Entregas atualizadas para ATRASADA: {}", atualizadas);
        return atualizadas;
    }
}
