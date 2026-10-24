package com.desafio.obrigacoes.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TesteController {

    @GetMapping("/")
    public String inicio() {
        return "API de Obrigações funcionando";
    }
}
