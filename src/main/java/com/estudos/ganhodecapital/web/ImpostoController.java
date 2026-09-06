package com.estudos.ganhodecapital.web;

import com.estudos.ganhodecapital.domain.Imposto;
import com.estudos.ganhodecapital.domain.Operacao;
import com.estudos.ganhodecapital.service.ImpostoService;
import com.estudos.ganhodecapital.web.dto.ImpostoResponse;
import com.estudos.ganhodecapital.web.dto.OperacaoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/impostos", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Impostos", description = "Calculo de imposto sobre ganho de capital")
public class ImpostoController {

    private final ImpostoService service;

    public ImpostoController(ImpostoService service) {
        this.service = service;
    }

    /**
     * Uma simulacao: recebe a lista de operacoes e devolve o imposto de cada uma.
     *
     * <pre>
     * POST /api/impostos/simulacao
     * [ {"operation":"buy","unit-cost":10.00,"quantity":100},
     *   {"operation":"sell","unit-cost":15.00,"quantity":50} ]
     * </pre>
     */
    @PostMapping(path = "/simulacao", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Calcula o imposto de cada operacao de uma simulacao e a salva no historico")
    public List<ImpostoResponse> calcularSimulacao(
            @RequestBody @NotEmpty(message = "informe ao menos uma operacao")
            List<@Valid OperacaoRequest> operacoes) {

        List<Operacao> dominio = operacoes.stream().map(OperacaoRequest::paraDominio).toList();
        return toResponse(service.calcularSimulacao(dominio));
    }

    /**
     * Lote de simulacoes: cada elemento e uma lista independente de operacoes,
     * como as varias linhas da entrada do desafio.
     *
     * <pre>
     * POST /api/impostos/lote
     * [ [ {"operation":"buy",...}, {"operation":"sell",...} ],
     *   [ {"operation":"buy",...}, {"operation":"sell",...} ] ]
     * </pre>
     */
    @PostMapping(path = "/lote", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Calcula varias simulacoes independentes de uma vez (uma lista por linha do desafio)")
    public List<List<ImpostoResponse>> calcularLote(
            @RequestBody @NotEmpty(message = "informe ao menos uma simulacao")
            List<@NotEmpty List<@Valid OperacaoRequest>> simulacoes) {

        List<List<Operacao>> dominio = simulacoes.stream()
                .map(lista -> lista.stream().map(OperacaoRequest::paraDominio).toList())
                .toList();

        return service.calcularLote(dominio).stream()
                .map(this::toResponse)
                .toList();
    }

    private List<ImpostoResponse> toResponse(List<Imposto> impostos) {
        return impostos.stream().map(ImpostoResponse::de).toList();
    }
}
