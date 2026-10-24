package com.desafio.obrigacoes.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.desafio.obrigacoes.TestcontainersConfiguration;
import com.desafio.obrigacoes.config.RelogioFixoTestConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, RelogioFixoTestConfig.class})
class UploadServidorRealIT {

    private static final String BOUNDARY = "XBOUNDARYX";

    @Value("${local.server.port}") int porta;

    private HttpResponse<String> enviar(String contentType, byte[] corpo) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/entregas/1/documentos"))
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofByteArray(corpo))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static byte[] multipartComArquivoDe(int bytes) {
        byte[] cabecalho = ("--" + BOUNDARY + "\r\nContent-Disposition: form-data; name=\"arquivo\"; filename=\"grande.pdf\"\r\n"
                + "Content-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] rodape = ("\r\n--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] corpo = new byte[cabecalho.length + bytes + rodape.length];
        System.arraycopy(cabecalho, 0, corpo, 0, cabecalho.length);
        System.arraycopy(rodape, 0, corpo, cabecalho.length + bytes, rodape.length);
        return corpo;
    }

    @Test
    void arquivoMuitoAcimaDoLimiteDevolve400() throws Exception {
        HttpResponse<String> resposta = enviar("multipart/form-data; boundary=" + BOUNDARY,
                multipartComArquivoDe(30 * 1024 * 1024));

        assertThat(resposta.statusCode()).isEqualTo(400);
        assertThat(resposta.body()).contains("O arquivo excede o limite de 10 MB");
    }

    @Test
    void multipartSemBoundaryDevolve400() throws Exception {
        HttpResponse<String> resposta = enviar("multipart/form-data", "x".getBytes(StandardCharsets.UTF_8));

        assertThat(resposta.statusCode()).isEqualTo(400);
    }
}
