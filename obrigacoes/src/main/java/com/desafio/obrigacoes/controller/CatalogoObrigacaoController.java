package com.desafio.obrigacoes.controller;

import com.desafio.obrigacoes.dto.TipoObrigacaoResponse;
import com.desafio.obrigacoes.model.TipoObrigacao;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Arrays;
import java.util.List;

@RestController
@Tag(name = "Catálogo", description = "Tipos de obrigação e regimes tributários aceitos")
public class CatalogoObrigacaoController {

    @GetMapping("/catalogo-obrigacoes")
    public List<TipoObrigacaoResponse> listar() {
        return Arrays.stream(TipoObrigacao.values()).map(TipoObrigacaoResponse::de).toList();
    }
}
