package com.estudos.ganhodecapital.web;

import com.estudos.ganhodecapital.application.HistoricoService;
import com.estudos.ganhodecapital.historico.SimulacaoDetalhe;
import com.estudos.ganhodecapital.historico.SimulacaoResumo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/simulacoes", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Historico", description = "Consulta de simulacoes ja calculadas")
public class HistoricoController {

    private final HistoricoService service;

    public HistoricoController(HistoricoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista as simulacoes salvas, da mais recente para a mais antiga")
    public List<SimulacaoResumo> listar(
            @ParameterObject @PageableDefault(size = 20) Pageable pagina) {
        return service.listar(pagina);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma simulacao: cada operacao e o imposto que gerou")
    public SimulacaoDetalhe buscar(@PathVariable Long id) {
        return service.detalhar(id);
    }
}
