// account-module/src/main/java/com/coreledger/account/domain/model/AccountStatus.java
package com.coreledger.account.domain.model;

/**
 * Lifecycle states of an Account.
 *
 * ACTIVE — normal operating state, deposits/withdrawals/transfers permitted
 * FROZEN — temporarily locked (e.g. compliance hold), no debits allowed,
 * credits may be permitted depending on policy
 * CLOSED — permanently closed, no operations permitted
 *
 * State transitions (enforced in Account aggregate, not here):
 * ACTIVE → FROZEN → ACTIVE (freeze / unfreeze)
 * ACTIVE → CLOSED (close)
 * FROZEN → CLOSED (close while frozen)
 * CLOSED → * (no transitions out of CLOSED — terminal state)
 */
public enum AccountStatus {
    ACTIVE,
    FROZEN,
    CLOSED
}