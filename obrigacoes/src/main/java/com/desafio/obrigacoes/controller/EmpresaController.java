package com.desafio.obrigacoes.controller;

import com.desafio.obrigacoes.dto.EmpresaRequest;
import com.desafio.obrigacoes.dto.EmpresaResponse;
import com.desafio.obrigacoes.dto.AlterarAtivaRequest;
import com.desafio.obrigacoes.service.EmpresaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/empresas")
public class EmpresaController {

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @GetMapping
    public List<EmpresaResponse> listarTodas(@RequestParam(defaultValue = "false") boolean incluirInativas) {
        return empresaService.listarTodas(incluirInativas).stream().map(EmpresaResponse::de).toList();
    }

    @GetMapping("/{id}")
    public EmpresaResponse buscarPorId(@PathVariable Long id) {
        return EmpresaResponse.de(empresaService.buscarPorId(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmpresaResponse cadastrar(@RequestBody EmpresaRequest request) {
        return EmpresaResponse.de(empresaService.cadastrar(request));
    }

    @PutMapping("/{id}")
    public EmpresaResponse editar(@PathVariable Long id, @RequestBody EmpresaRequest request) {
        return EmpresaResponse.de(empresaService.editar(id, request));
    }

    @PatchMapping("/{id}/ativa")
    public EmpresaResponse alterarAtiva(
            @PathVariable Long id,
            @RequestBody(required = false) AlterarAtivaRequest request) {
        return EmpresaResponse.de(empresaService.alterarAtiva(id, request == null ? null : request.ativa()));
    }
}
