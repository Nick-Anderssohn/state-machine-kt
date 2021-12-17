package com.mrstatemachine.engine

internal data class StateStore<TStateBase : Any, TExtendedState : Any> (
    val extendedStateStore: ExtendedStateStore<TExtendedState>
) {
    @Volatile
    lateinit var currentState: TStateBase
}
