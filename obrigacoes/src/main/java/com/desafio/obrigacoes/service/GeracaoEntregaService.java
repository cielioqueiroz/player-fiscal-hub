package com.desafio.obrigacoes.service;

import com.desafio.obrigacoes.dto.ResumoGeracaoResponse;
import com.desafio.obrigacoes.model.EmpresaObrigacao;
import com.desafio.obrigacoes.model.TipoObrigacao;
import com.desafio.obrigacoes.repository.EmpresaObrigacaoRepository;
import com.desafio.obrigacoes.repository.EntregaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
public class GeracaoEntregaService {

    private final EmpresaObrigacaoRepository empresaObrigacaoRepository;
    private final EntregaRepository entregaRepository;
    private final Clock clock;

    public GeracaoEntregaService(EmpresaObrigacaoRepository empresaObrigacaoRepository,
                                 EntregaRepository entregaRepository,
                                 Clock clock) {
        this.empresaObrigacaoRepository = empresaObrigacaoRepository;
        this.entregaRepository = entregaRepository;
        this.clock = clock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ResumoGeracaoResponse gerar(YearMonth mesReferencia) {
        LocalDateTime agora = LocalDateTime.now(clock);
        int criadas = 0;
        int jaExistentes = 0;

        for (EmpresaObrigacao vinculo : empresaObrigacaoRepository.findAllByOrderByEmpresaIdAscTipoAsc()) {
            TipoObrigacao tipo = vinculo.getTipo();
            String competencia = tipo.competenciaPara(mesReferencia);
            LocalDate vencimento = tipo.calcularVencimento(competencia);

            int linhas = entregaRepository.inserirSeNaoExistir(
                    vinculo.getEmpresa().getId(), tipo.name(), competencia, vencimento, agora);

            if (linhas == 1) {
                criadas++;
            } else {
                jaExistentes++;
            }
        }

        return new ResumoGeracaoResponse(mesReferencia.toString(), criadas, jaExistentes);
    }
}
