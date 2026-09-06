package com.example.capitalgains.domain.portfolio;

/**
 * States of the {@link Portfolio}.
 *
 * <pre>
 *              Buy                             Buy (recalculates average price)
 *          ┌────────────┐                     ┌───────────┐
 *          ▼            │                     ▼           │
 *      ┌───────┐   Buy      ┌──────────┐                  │
 *      │ EMPTY │ ─────────► │ HOLDING  │ ─────────────────┘
 *      └───────┘            └──────────┘
 *          ▲                   │   │
 *          │  Sell (empties)   │   │  Partial sell
 *          └───────────────────┘   │  (stays HOLDING)
 *                                  │
 *      Sell while EMPTY  ─────►  SellExceedsPortfolioException
 * </pre>
 *
 * The accumulated loss is NOT a state: it is data (extended state) that the
 * transition actions read and write, alongside average price and quantity.
 */
public enum PortfolioState {

    /** No position: zero quantity and no average price defined. */
    EMPTY,

    /** Shares are held, with a weighted average price. */
    HOLDING
}
