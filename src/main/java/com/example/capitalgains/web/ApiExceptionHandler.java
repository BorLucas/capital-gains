package com.example.capitalgains.web;

import com.example.capitalgains.domain.error.CapitalGainsException;
import com.example.capitalgains.domain.error.InvalidTradeException;
import com.example.capitalgains.domain.error.SellExceedsPortfolioException;
import com.example.capitalgains.history.SimulationNotFoundException;
import com.example.capitalgains.orders.OrderNotFoundException;
import jakarta.validation.ConstraintViolationException;
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
 * Translates exceptions into RFC 7807 responses. Each error type has its own
 * status; no generic {@code IllegalArgumentException} as a business-error
 * channel.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({SimulationNotFoundException.class, OrderNotFoundException.class})
    public ProblemDetail notFound(RuntimeException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(SellExceedsPortfolioException.class)
    public ProblemDetail businessRule(SellExceedsPortfolioException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(InvalidTradeException.class)
    public ProblemDetail invalidTrade(InvalidTradeException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Safety net for any other domain exception. */
    @ExceptionHandler(CapitalGainsException.class)
    public ProblemDetail domain(CapitalGainsException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalidBody(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getAllErrors().stream()
                .map(e -> e instanceof FieldError fe
                        ? fe.getField() + ": " + fe.getDefaultMessage()
                        : e.getDefaultMessage())
                .toList();
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "invalid fields");
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail invalidParameter(HandlerMethodValidationException ex) {
        return problem(HttpStatus.BAD_REQUEST, "invalid parameters");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail constraintViolation(ConstraintViolationException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail unreadableJson(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, "malformed JSON in request body");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail methodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail mediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
