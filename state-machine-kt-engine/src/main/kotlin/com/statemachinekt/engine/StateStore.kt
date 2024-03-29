package com.statemachinekt.engine

internal data class StateStore<TStateBase : Any, TExtendedState : Any> (
    internal val acceptingState: TStateBase,
    internal val extendedStateStore: ExtendedStateStore<TExtendedState>
)
