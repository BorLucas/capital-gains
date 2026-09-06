package com.estudos.ganhodecapital.web;

import com.estudos.ganhodecapital.application.CalcularImpostoService;
import com.estudos.ganhodecapital.application.SimulacaoCalculada;
import com.estudos.ganhodecapital.domain.Operacao;
import com.estudos.ganhodecapital.formato.ImpostoJson;
import com.estudos.ganhodecapital.formato.OperacaoJson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(value = "/api/impostos", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Impostos", description = "Calculo de imposto sobre ganho de capital")
public class ImpostoController {

    private final CalcularImpostoService service;

    public ImpostoController(CalcularImpostoService service) {
        this.service = service;
    }

    /**
     * Uma simulacao: calcula o imposto de cada operacao e guarda no historico.
     * Responde {@code 201 Created} com {@code Location} apontando para a
     * simulacao salva; o corpo mantem o formato do desafio.
     */
    @PostMapping(path = "/simulacao", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Calcula o imposto de cada operacao de uma simulacao e a salva no historico")
    public ResponseEntity<List<ImpostoJson>> calcularSimulacao(
            @RequestBody @NotEmpty(message = "informe ao menos uma operacao")
            List<@Valid OperacaoJson> operacoes) {

        List<Operacao> dominio = OperacaoJson.paraDominio(operacoes);
        SimulacaoCalculada calculada = service.calcular(dominio);

        return ResponseEntity
                .created(URI.create("/api/simulacoes/" + calculada.id()))
                .body(ImpostoJson.deResultados(calculada.resultados()));
    }

    /**
     * Lote de simulacoes independentes, como as varias linhas da entrada do
     * desafio. Calcula tudo antes de gravar; grava tudo numa transacao.
     */
    @PostMapping(path = "/lote", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Calcula varias simulacoes independentes de uma vez (uma lista por linha do desafio)")
    public List<List<ImpostoJson>> calcularLote(
            @RequestBody @NotEmpty(message = "informe ao menos uma simulacao")
            List<@NotEmpty List<@Valid OperacaoJson>> simulacoes) {

        List<List<Operacao>> dominio = simulacoes.stream()
                .map(OperacaoJson::paraDominio)
                .toList();

        return service.calcularLote(dominio).stream()
                .map(calculada -> ImpostoJson.deResultados(calculada.resultados()))
                .toList();
    }
}
