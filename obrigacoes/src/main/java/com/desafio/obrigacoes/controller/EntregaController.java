package com.desafio.obrigacoes.controller;

import com.desafio.obrigacoes.dto.AlterarSituacaoRequest;
import com.desafio.obrigacoes.dto.EntregaResponse;
import com.desafio.obrigacoes.dto.GerarEntregasRequest;
import com.desafio.obrigacoes.dto.ResumoGeracaoResponse;
import com.desafio.obrigacoes.dto.ResumoEntregasResponse;
import com.desafio.obrigacoes.dto.PaginaResponse;
import com.desafio.obrigacoes.exception.DadosInvalidosException;
import com.desafio.obrigacoes.model.SituacaoEntrega;
import com.desafio.obrigacoes.model.TipoObrigacao;
import com.desafio.obrigacoes.service.EntregaService;
import com.desafio.obrigacoes.service.GeracaoEntregaService;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/entregas")
public class EntregaController {

    private static final int ANO_MINIMO = 2000;
    private static final int ANO_MAXIMO = 2999;
    private static final int TAMANHO_MAXIMO_PAGINA = 100;
    private static final Set<String> CAMPOS_ORDENACAO = Set.of(
            "dataVencimento", "competencia", "tipo", "situacao", "empresa");

    private final GeracaoEntregaService geracaoEntregaService;
    private final EntregaService entregaService;
    private final Clock clock;

    public EntregaController(GeracaoEntregaService geracaoEntregaService, EntregaService entregaService, Clock clock) {
        this.geracaoEntregaService = geracaoEntregaService;
        this.entregaService = entregaService;
        this.clock = clock;
    }

    @PostMapping("/gerar")
    public ResumoGeracaoResponse gerar(@RequestBody(required = false) GerarEntregasRequest request) {
        return geracaoEntregaService.gerar(mesReferencia(request));
    }

    @GetMapping
    public PaginaResponse<EntregaResponse> listar(
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) TipoObrigacao tipo,
            @RequestParam(required = false) String competencia,
            @RequestParam(required = false) SituacaoEntrega situacao,
            @PageableDefault(size = 20, sort = "dataVencimento") Pageable pageable) {

        if (pageable.getPageSize() > TAMANHO_MAXIMO_PAGINA) {
            throw new DadosInvalidosException("O tamanho máximo da página é 100");
        }
        Pageable paginacao = paginacaoEstavel(pageable);

        String competenciaFiltro = competencia == null || competencia.isBlank() ? null : competencia.trim();

        LocalDate hoje = LocalDate.now(clock);
        return PaginaResponse.de(entregaService.listar(empresaId, tipo, competenciaFiltro, situacao, paginacao),
                e -> EntregaResponse.de(e, hoje));
    }

    @GetMapping("/resumo")
    public ResumoEntregasResponse resumir(@RequestParam(required = false) Long empresaId) {
        return entregaService.resumir(empresaId);
    }

    @GetMapping("/{id}")
    public EntregaResponse detalhar(@PathVariable Long id) {
        return EntregaResponse.de(entregaService.buscarPorId(id), LocalDate.now(clock));
    }

    @PatchMapping("/{id}/situacao")
    public EntregaResponse alterarSituacao(@PathVariable Long id,
                                           @RequestBody(required = false) AlterarSituacaoRequest request) {
        return entregaService.alterarSituacao(id, request);
    }

    private static YearMonth mesReferencia(GerarEntregasRequest request) {
        if (request == null || request.mesReferencia() == null) {
            throw new DadosInvalidosException("mesReferencia é obrigatório (formato AAAA-MM)");
        }

        YearMonth mes;
        try {
            mes = YearMonth.parse(request.mesReferencia());
        } catch (DateTimeParseException e) {
            throw new DadosInvalidosException("mesReferencia deve estar no formato AAAA-MM");
        }

        if (mes.getYear() < ANO_MINIMO || mes.getYear() > ANO_MAXIMO) {
            throw new DadosInvalidosException(
                    "mesReferencia deve ter ano entre " + ANO_MINIMO + " e " + ANO_MAXIMO);
        }
        return mes;
    }

    private static Pageable paginacaoEstavel(Pageable pageable) {
        List<Sort.Order> ordens = new java.util.ArrayList<>();
        for (Sort.Order ordem : pageable.getSort()) {
            if (!CAMPOS_ORDENACAO.contains(ordem.getProperty())) {
                throw new DadosInvalidosException("Ordenação permitida: dataVencimento, competencia, tipo, situacao e empresa");
            }
            String campo = ordem.getProperty().equals("empresa") ? "empresa.nome" : ordem.getProperty();
            ordens.add(new Sort.Order(ordem.getDirection(), campo, ordem.getNullHandling()));
        }
        if (ordens.stream().noneMatch(ordem -> ordem.getProperty().equals("id"))) {
            ordens.add(Sort.Order.asc("id"));
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(ordens));
    }
}
