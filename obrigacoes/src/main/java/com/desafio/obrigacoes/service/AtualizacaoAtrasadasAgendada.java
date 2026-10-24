package com.desafio.obrigacoes.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "obrigacoes.atualizacao-atrasadas", name = "habilitada", havingValue = "true", matchIfMissing = true)
public class AtualizacaoAtrasadasAgendada {

    private final AtualizacaoAtrasadasService atualizacaoAtrasadasService;

    public AtualizacaoAtrasadasAgendada(AtualizacaoAtrasadasService atualizacaoAtrasadasService) {
        this.atualizacaoAtrasadasService = atualizacaoAtrasadasService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void aoIniciar() {
        atualizacaoAtrasadasService.atualizarAtrasadas();
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "America/Sao_Paulo")
    public void diariamente() {
        atualizacaoAtrasadasService.atualizarAtrasadas();
    }
}
