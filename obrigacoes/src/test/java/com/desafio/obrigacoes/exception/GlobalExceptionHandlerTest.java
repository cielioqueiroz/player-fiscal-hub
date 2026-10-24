package com.desafio.obrigacoes.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.desafio.obrigacoes.dto.ErroResponse;
import com.desafio.obrigacoes.model.Entrega;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

    @Test
    void conflitoDeVersaoDevolve409() {
        ResponseEntity<ErroResponse> resposta = new GlobalExceptionHandler()
                .conflitoDeVersao(new ObjectOptimisticLockingFailureException(Entrega.class, 1L));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().erro())
                .isEqualTo("A entrega foi alterada por outra pessoa. Recarregue e tente novamente.");
    }

    @Test
    void bloqueioExpiradoDevolve409() {
        ResponseEntity<ErroResponse> resposta = new GlobalExceptionHandler()
                .bloqueioIndisponivel(new CannotAcquireLockException("Lock wait timeout exceeded"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().erro())
                .isEqualTo("O recurso está sendo alterado por outra operação. Tente novamente.");
    }

    @Test
    void arquivoGrandeDevolve400() {
        ResponseEntity<ErroResponse> resposta = new GlobalExceptionHandler()
                .arquivoGrande(new MaxUploadSizeExceededException(10L * 1024 * 1024));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().erro()).isEqualTo("O arquivo excede o limite de 10 MB");
    }
}
