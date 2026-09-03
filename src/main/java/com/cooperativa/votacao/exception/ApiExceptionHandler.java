package com.cooperativa.votacao.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleNaoEncontrado(RecursoNaoEncontradoException ex) {
        log.warn("Recurso nao encontrado: {}", ex.getMessage());
        return responder(HttpStatus.NOT_FOUND, "Recurso nao encontrado", ex.getMessage(), null);
    }

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErrorResponse> handleNegocio(NegocioException ex) {
        log.warn("Regra de negocio violada: {}", ex.getMessage());
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negocio violada", ex.getMessage(), null);
    }

    @ExceptionHandler(AssociadoNaoAptoException.class)
    public ResponseEntity<ErrorResponse> handleAssociadoNaoApto(AssociadoNaoAptoException ex) {
        log.warn("Associado nao apto a votar: {}", ex.getMessage());
        return responder(HttpStatus.FORBIDDEN, "Associado nao apto a votar", ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidacao(MethodArgumentNotValidException ex) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "Erro de validacao", "Um ou mais campos sao invalidos", detalhes);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Erro de validacao", ex.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenerico(Exception ex) {
        log.error("Erro inesperado", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Ocorreu um erro inesperado", null);
    }

    private ResponseEntity<ErrorResponse> responder(HttpStatus status, String erro, String mensagem, List<String> detalhes) {
        ErrorResponse body = new ErrorResponse(LocalDateTime.now(), status.value(), erro, mensagem, detalhes);
        return ResponseEntity.status(status).body(body);
    }
}