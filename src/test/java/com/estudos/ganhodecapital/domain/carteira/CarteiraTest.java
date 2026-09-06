package com.estudos.ganhodecapital.domain.carteira;

import com.estudos.ganhodecapital.domain.Dinheiro;
import com.estudos.ganhodecapital.domain.Operacao;
import com.estudos.ganhodecapital.domain.erro.VendaSuperaCarteiraException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CarteiraTest {

    private final Carteira carteira = new Carteira();

    private void aplicar(Operacao operacao) {
        carteira.aplicar(EventoOperacao.de(operacao));
    }

    @Test
    @DisplayName("comeca VAZIA")
    void comecaVazia() {
        assertThat(carteira.estado()).isEqualTo(EstadoCarteira.VAZIA);
        assertThat(carteira.quantidade()).isZero();
    }

    @Test
    @DisplayName("VAZIA --Comprar--> COMPRADA define o preco medio")
    void compraDefinePrecoMedio() {
        aplicar(Operacao.compra("10.00", 100));

        assertThat(carteira.estado()).isEqualTo(EstadoCarteira.COMPRADA);
        assertThat(carteira.quantidade()).isEqualTo(100);
        assertThat(carteira.precoMedio()).isEqualTo(Dinheiro.de("10.00"));
    }

    @Test
    @DisplayName("COMPRADA --Comprar--> COMPRADA recalcula o preco medio ponderado")
    void compraRecalculaPrecoMedio() {
        aplicar(Operacao.compra("10.00", 10000));
        aplicar(Operacao.compra("25.00", 5000));

        assertThat(carteira.precoMedio()).isEqualTo(Dinheiro.de("15.00"));
        assertThat(carteira.quantidade()).isEqualTo(15000);
    }

    @Test
    @DisplayName("COMPRADA --Vender total--> volta para VAZIA e zera o preco medio")
    void vendaTotalVoltaParaVazia() {
        aplicar(Operacao.compra("10.00", 100));
        aplicar(Operacao.venda("50.00", 100));

        assertThat(carteira.estado()).isEqualTo(EstadoCarteira.VAZIA);
        assertThat(carteira.quantidade()).isZero();
        assertThat(carteira.precoMedio()).isEqualTo(Dinheiro.ZERO);
    }

    @Test
    @DisplayName("COMPRADA --Vender parcial--> permanece COMPRADA com mesmo preco medio")
    void vendaParcialPermaneceComprada() {
        aplicar(Operacao.compra("10.00", 100));
        aplicar(Operacao.venda("50.00", 40));

        assertThat(carteira.estado()).isEqualTo(EstadoCarteira.COMPRADA);
        assertThat(carteira.quantidade()).isEqualTo(60);
        assertThat(carteira.precoMedio()).isEqualTo(Dinheiro.de("10.00"));
    }

    @Test
    @DisplayName("transicao ilegal: Vender em carteira VAZIA")
    void venderEmCarteiraVazia() {
        assertThatThrownBy(() -> aplicar(Operacao.venda("10.00", 1)))
                .isInstanceOf(VendaSuperaCarteiraException.class);
    }

    @Test
    @DisplayName("guarda falha antes de mexer no estado")
    void guardaNaoAlteraEstado() {
        aplicar(Operacao.compra("10.00", 100));

        assertThatThrownBy(() -> aplicar(Operacao.venda("10.00", 500)))
                .isInstanceOf(VendaSuperaCarteiraException.class);

        assertThat(carteira.quantidade()).isEqualTo(100);
        assertThat(carteira.estado()).isEqualTo(EstadoCarteira.COMPRADA);
    }

    @Test
    @DisplayName("prejuizo fica acumulado no estado estendido")
    void prejuizoAcumulado() {
        aplicar(Operacao.compra("10.00", 10000));
        aplicar(Operacao.venda("2.00", 5000)); // prejuizo (10-2)*5000 = 40000

        assertThat(carteira.prejuizoAcumulado()).isEqualTo(Dinheiro.de("40000.00"));
    }

    @Test
    @DisplayName("compra sempre gera imposto zero")
    void compraImpostoZero() {
        var imposto = carteira.aplicar(EventoOperacao.de(Operacao.compra("10.00", 100)));
        assertThat(imposto.emReais().signum()).isZero();
    }
}
