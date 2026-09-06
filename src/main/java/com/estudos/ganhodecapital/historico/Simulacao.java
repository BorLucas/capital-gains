package com.estudos.ganhodecapital.historico;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Uma simulacao ja calculada e guardada no historico: a sequencia de operacoes
 * enviada e o imposto resultante de cada uma.
 */
@Entity
@Table(name = "simulacao")
public class Simulacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "imposto_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal impostoTotal;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "simulacao_item", joinColumns = @JoinColumn(name = "simulacao_id"))
    @OrderColumn(name = "posicao")
    private List<ItemSimulacao> itens = new ArrayList<>();

    protected Simulacao() {
        // exigido pelo JPA
    }

    public Simulacao(List<ItemSimulacao> itens) {
        this.criadaEm = Instant.now();
        this.itens = new ArrayList<>(itens);
        this.impostoTotal = itens.stream()
                .map(ItemSimulacao::getImposto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() {
        return id;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public BigDecimal getImpostoTotal() {
        return impostoTotal;
    }

    public List<ItemSimulacao> getItens() {
        return List.copyOf(itens);
    }
}
