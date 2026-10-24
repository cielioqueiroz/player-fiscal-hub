package com.desafio.obrigacoes.dto;

import com.desafio.obrigacoes.model.RegimeTributario;

public record EmpresaRequest(String nome, String cnpj, RegimeTributario regimeTributario) {
}
