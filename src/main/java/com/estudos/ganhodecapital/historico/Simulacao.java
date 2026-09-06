package com.estudos.ganhodecapital.historico;

import com.estudos.ganhodecapital.domain.ResultadoOperacao;
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
 *
 * <p>{@code quantidadeOperacoes} e {@code impostoTotal} sao desnormalizados e
 * calculados uma vez na criacao (a simulacao e imutavel depois de salva). Isso
 * permite a listagem consultar so o resumo, sem carregar a colecao de itens.</p>
 */
@Entity
@Table(name = "simulacao")
public class Simulacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "quantidade_operacoes", nullable = false)
    private int quantidadeOperacoes;

    @Column(name = "imposto_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal impostoTotal;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "simulacao_item", joinColumns = @JoinColumn(name = "simulacao_id"))
    @OrderColumn(name = "posicao")
    private List<ItemSimulacao> itens = new ArrayList<>();

    protected Simulacao() {
        // exigido pelo JPA
    }

    private Simulacao(List<ItemSimulacao> itens, BigDecimal impostoTotal, Instant criadaEm) {
        this.itens = new ArrayList<>(itens);
        this.quantidadeOperacoes = itens.size();
        this.impostoTotal = impostoTotal;
        this.criadaEm = criadaEm;
    }

    public static Simulacao de(List<ResultadoOperacao> resultados, Instant criadaEm) {
        List<ItemSimulacao> itens = resultados.stream().map(ItemSimulacao::de).toList();
        BigDecimal total = resultados.stream()
                .map(r -> r.imposto().emReais())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Simulacao(itens, total, criadaEm);
    }

    public Long getId() {
        return id;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public int getQuantidadeOperacoes() {
        return quantidadeOperacoes;
    }

    public BigDecimal getImpostoTotal() {
        return impostoTotal;
    }

    public List<ItemSimulacao> getItens() {
        return List.copyOf(itens);
    }
}
