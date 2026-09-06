package com.estudos.ganhodecapital.web;

import com.estudos.ganhodecapital.domain.erro.GanhoDeCapitalException;
import com.estudos.ganhodecapital.domain.erro.OperacaoInvalidaException;
import com.estudos.ganhodecapital.domain.erro.VendaSuperaCarteiraException;
import com.estudos.ganhodecapital.historico.SimulacaoNaoEncontradaException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;

/**
 * Traducao de excecoes para respostas RFC 7807. Cada tipo de erro tem seu
 * status; nada de {@code IllegalArgumentException} generica como canal de
 * erro de negocio.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(SimulacaoNaoEncontradaException.class)
    public ProblemDetail naoEncontrada(SimulacaoNaoEncontradaException ex) {
        return problema(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(VendaSuperaCarteiraException.class)
    public ProblemDetail regraDeNegocio(VendaSuperaCarteiraException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(OperacaoInvalidaException.class)
    public ProblemDetail operacaoInvalida(OperacaoInvalidaException ex) {
        return problema(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Rede de seguranca para qualquer outra excecao de dominio. */
    @ExceptionHandler(GanhoDeCapitalException.class)
    public ProblemDetail dominio(GanhoDeCapitalException ex) {
        return problema(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail corpoInvalido(MethodArgumentNotValidException ex) {
        List<String> erros = ex.getBindingResult().getAllErrors().stream()
                .map(e -> e instanceof FieldError fe
                        ? fe.getField() + ": " + fe.getDefaultMessage()
                        : e.getDefaultMessage())
                .toList();
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "campos invalidos");
        pd.setProperty("errors", erros);
        return pd;
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail parametroInvalido(HandlerMethodValidationException ex) {
        return problema(HttpStatus.BAD_REQUEST, "parametros invalidos");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail violacaoDeConstraint(ConstraintViolationException ex) {
        return problema(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail jsonIlegivel(HttpMessageNotReadableException ex) {
        return problema(HttpStatus.BAD_REQUEST, "JSON invalido no corpo da requisicao");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail metodoNaoSuportado(HttpRequestMethodNotSupportedException ex) {
        return problema(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail midiaNaoSuportada(HttpMediaTypeNotSupportedException ex) {
        return problema(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail erroInesperado(Exception ex) {
        log.error("erro nao tratado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "erro interno");
    }

    private static ProblemDetail problema(HttpStatus status, String detalhe) {
        return ProblemDetail.forStatusAndDetail(status, detalhe);
    }
}
