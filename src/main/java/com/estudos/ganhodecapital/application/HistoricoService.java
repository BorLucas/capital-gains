package com.estudos.ganhodecapital.application;

import com.estudos.ganhodecapital.domain.ResultadoOperacao;
import com.estudos.ganhodecapital.historico.Simulacao;
import com.estudos.ganhodecapital.historico.SimulacaoDetalhe;
import com.estudos.ganhodecapital.historico.SimulacaoNaoEncontradaException;
import com.estudos.ganhodecapital.historico.SimulacaoRepository;
import com.estudos.ganhodecapital.historico.SimulacaoResumo;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Guarda e consulta simulacoes ja calculadas. A borda web recebe read models
 * ({@link SimulacaoResumo} / {@link SimulacaoDetalhe}), nunca a entidade.
 */
@Service
public class HistoricoService {

    private final SimulacaoRepository repository;
    private final Clock clock;

    public HistoricoService(SimulacaoRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Simulacao registrar(List<ResultadoOperacao> resultados) {
        return repository.save(Simulacao.de(resultados, Instant.now(clock)));
    }

    /** Grava varias simulacoes numa unica transacao (tudo ou nada). */
    @Transactional
    public List<Long> registrarTodas(List<List<ResultadoOperacao>> simulacoes) {
        return simulacoes.stream()
                .map(resultados -> registrar(resultados).getId())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SimulacaoResumo> listar(Pageable pagina) {
        return repository.findAllByOrderByIdDesc(pagina);
    }

    @Transactional(readOnly = true)
    public SimulacaoDetalhe detalhar(Long id) {
        return repository.findById(id)
                .map(SimulacaoDetalhe::de)
                .orElseThrow(() -> new SimulacaoNaoEncontradaException(id));
    }
}
