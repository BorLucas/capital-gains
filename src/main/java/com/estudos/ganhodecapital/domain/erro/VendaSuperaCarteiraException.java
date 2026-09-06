package com.estudos.ganhodecapital.domain.erro;

/**
 * Tentativa de vender mais acoes do que existem em carteira. A requisicao esta
 * bem formada, mas viola uma regra de negocio -> mapeada para HTTP 422.
 */
public class VendaSuperaCarteiraException extends GanhoDeCapitalException {

    public VendaSuperaCarteiraException(long quantidadeVendida, long quantidadeEmCarteira) {
        super("venda de %d acoes, mas ha apenas %d em carteira"
                .formatted(quantidadeVendida, quantidadeEmCarteira));
    }
}
