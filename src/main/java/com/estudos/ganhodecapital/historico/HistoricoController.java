package com.estudos.ganhodecapital.historico;

import com.estudos.ganhodecapital.historico.dto.SimulacaoDetalheResponse;
import com.estudos.ganhodecapital.historico.dto.SimulacaoResumoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    public List<SimulacaoResumoResponse> listar() {
        return service.listar().stream().map(SimulacaoResumoResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma simulacao: cada operacao e o imposto que gerou")
    public SimulacaoDetalheResponse buscar(@PathVariable Long id) {
        return SimulacaoDetalheResponse.de(service.buscar(id));
    }
}
