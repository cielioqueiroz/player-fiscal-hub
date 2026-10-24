package com.desafio.obrigacoes.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI obrigacoesOpenApi() {
        return new OpenAPI().info(new Info()
                .title("API do Gerenciador de Obrigações")
                .description("API para empresas, obrigações fiscais, entregas e documentos.")
                .version("1.0.0"));
    }
}
