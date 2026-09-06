package com.estudos.ganhodecapital.historico;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SimulacaoRepository extends JpaRepository<Simulacao, Long> {

    /** Listagem paginada usando a projecao de resumo (nao carrega os itens). */
    List<SimulacaoResumo> findAllByOrderByIdDesc(Pageable pageable);
}
