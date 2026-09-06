package com.estudos.ganhodecapital.historico;

import com.estudos.ganhodecapital.domain.Operacao;
import com.estudos.ganhodecapital.domain.ResultadoOperacao;
import com.estudos.ganhodecapital.domain.TipoOperacao;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;

/**
 * Uma operacao registrada dentro de uma {@link Simulacao}, junto com o imposto
 * que ela gerou.
 */
@Embeddable
public class ItemSimulacao {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoOperacao tipo;

    @Column(name = "custo_unitario", nullable = false, precision = 19, scale = 2)
    private BigDecimal custoUnitario;

    @Column(nullable = false)
    private long quantidade;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal imposto;

    protected ItemSimulacao() {
        // exigido pelo JPA
    }

    public ItemSimulacao(TipoOperacao tipo, BigDecimal custoUnitario, long quantidade, BigDecimal imposto) {
        this.tipo = tipo;
        this.custoUnitario = custoUnitario;
        this.quantidade = quantidade;
        this.imposto = imposto;
    }

    static ItemSimulacao de(ResultadoOperacao resultado) {
        Operacao operacao = resultado.operacao();
        return new ItemSimulacao(
                operacao.tipo(),
                operacao.custoUnitario().valor(),
                operacao.quantidade(),
                resultado.imposto().emReais());
    }

    public TipoOperacao getTipo() {
        return tipo;
    }

    public BigDecimal getCustoUnitario() {
        return custoUnitario;
    }

    public long getQuantidade() {
        return quantidade;
    }

    public BigDecimal getImposto() {
        return imposto;
    }
}
