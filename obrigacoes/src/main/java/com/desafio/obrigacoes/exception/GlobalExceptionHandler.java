package com.desafio.obrigacoes.exception;

import com.desafio.obrigacoes.dto.ErroResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DadosInvalidosException.class)
    ResponseEntity<ErroResponse> dadosInvalidos(DadosInvalidosException e) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflitoException.class)
    ResponseEntity<ErroResponse> conflito(ConflitoException e) {
        return resposta(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ErroResponse> conflitoDeVersao(ObjectOptimisticLockingFailureException e) {
        return resposta(HttpStatus.CONFLICT,
                "A entrega foi alterada por outra pessoa. Recarregue e tente novamente.");
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    ResponseEntity<ErroResponse> bloqueioIndisponivel(PessimisticLockingFailureException e) {
        return resposta(HttpStatus.CONFLICT, "O recurso está sendo alterado por outra operação. Tente novamente.");
    }

    @ExceptionHandler(RegraNegocioException.class)
    ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException e) {
        return resposta(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException e) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou com valor desconhecido");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErroResponse> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetro inválido: " + e.getName());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErroResponse> integridade(DataIntegrityViolationException e) {
        return resposta(HttpStatus.CONFLICT, "O registro conflita com um já existente");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErroResponse> arquivoGrande(MaxUploadSizeExceededException e) {
        return resposta(HttpStatus.BAD_REQUEST, "O arquivo excede o limite de 10 MB");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ResponseEntity<ErroResponse> arquivoAusente(MissingServletRequestPartException e) {
        return resposta(HttpStatus.BAD_REQUEST, "Arquivo é obrigatório (campo 'arquivo')");
    }

    @ExceptionHandler(MultipartException.class)
    ResponseEntity<ErroResponse> multipartInvalido(MultipartException e) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisição multipart inválida");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErroResponse> inesperado(Exception e) {
        if (e instanceof ErrorResponse erroDoSpring) {
            return resposta(erroDoSpring.getStatusCode(), "Requisição inválida");
        }
        log.error("Erro inesperado", e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente.");
    }

    private ResponseEntity<ErroResponse> resposta(HttpStatusCode status, String mensagem) {
        return ResponseEntity.status(status).body(new ErroResponse(mensagem));
    }
}
