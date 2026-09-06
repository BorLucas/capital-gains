package com.estudos.ganhodecapital.historico;

import com.estudos.ganhodecapital.domain.Imposto;
import com.estudos.ganhodecapital.domain.Operacao;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Guarda e consulta simulacoes ja calculadas.
 */
@Service
public class HistoricoService {

    private final SimulacaoRepository repository;

    public HistoricoService(SimulacaoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Simulacao registrar(List<Operacao> operacoes, List<Imposto> impostos) {
        if (operacoes.size() != impostos.size()) {
            throw new IllegalArgumentException("operacoes e impostos com tamanhos diferentes");
        }
        List<ItemSimulacao> itens = java.util.stream.IntStream.range(0, operacoes.size())
                .mapToObj(i -> {
                    Operacao op = operacoes.get(i);
                    return new ItemSimulacao(op.tipo(), op.custoUnitario(), op.quantidade(),
                            impostos.get(i).valor());
                })
                .toList();
        return repository.save(new Simulacao(itens));
    }

    @Transactional(readOnly = true)
    public List<Simulacao> listar() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional(readOnly = true)
    public Simulacao buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new SimulacaoNaoEncontradaException(id));
    }
}
