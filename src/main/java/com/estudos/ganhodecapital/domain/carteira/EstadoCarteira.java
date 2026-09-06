package com.estudos.ganhodecapital.domain.carteira;

/**
 * Estados da {@link Carteira}.
 *
 * <pre>
 *              Comprar                         Comprar (recalcula preco medio)
 *          ┌────────────┐                     ┌───────────┐
 *          ▼            │                     ▼           │
 *      ┌───────┐  Comprar   ┌──────────┐                  │
 *      │ VAZIA │ ─────────► │ COMPRADA │ ─────────────────┘
 *      └───────┘            └──────────┘
 *          ▲                   │   │
 *          │  Vender (zera)    │   │  Vender parcial
 *          └───────────────────┘   │  (permanece COMPRADA)
 *                                  │
 *      Vender em VAZIA  ─────►  VendaSuperaCarteiraException
 * </pre>
 *
 * O prejuizo acumulado NAO e um estado: e dado (extended state) que as acoes de
 * transicao leem e escrevem, junto de preco medio e quantidade.
 */
public enum EstadoCarteira {

    /** Sem posicao: quantidade zero e sem preco medio definido. */
    VAZIA,

    /** Ha acoes em carteira, com um preco medio ponderado. */
    COMPRADA
}
