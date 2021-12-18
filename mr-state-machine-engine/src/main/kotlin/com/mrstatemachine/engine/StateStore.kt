package com.mrstatemachine.engine

internal data class StateStore<TStateBase : Any, TExtendedState : Any> (
    val acceptingState: TStateBase,
    val extendedStateStore: ExtendedStateStore<TExtendedState>
)
