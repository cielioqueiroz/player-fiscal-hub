package com.desafio.obrigacoes.controller;

import com.desafio.obrigacoes.dto.AnexarObrigacaoRequest;
import com.desafio.obrigacoes.dto.ObrigacaoEmpresaResponse;
import com.desafio.obrigacoes.model.TipoObrigacao;
import com.desafio.obrigacoes.service.EmpresaObrigacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/empresas/{empresaId}/obrigacoes")
public class EmpresaObrigacaoController {

    private final EmpresaObrigacaoService empresaObrigacaoService;

    public EmpresaObrigacaoController(EmpresaObrigacaoService empresaObrigacaoService) {
        this.empresaObrigacaoService = empresaObrigacaoService;
    }

    @GetMapping
    public List<ObrigacaoEmpresaResponse> listar(@PathVariable Long empresaId) {
        return empresaObrigacaoService.listar(empresaId).stream().map(ObrigacaoEmpresaResponse::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ObrigacaoEmpresaResponse anexar(
            @PathVariable Long empresaId,
            @RequestBody AnexarObrigacaoRequest request) {

        return ObrigacaoEmpresaResponse.de(empresaObrigacaoService.anexar(empresaId, request.tipo()));
    }

    @DeleteMapping("/{tipo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long empresaId, @PathVariable TipoObrigacao tipo) {
        empresaObrigacaoService.remover(empresaId, tipo);
    }
}
