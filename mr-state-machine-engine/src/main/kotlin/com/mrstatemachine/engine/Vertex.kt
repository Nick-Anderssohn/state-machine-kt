package com.mrstatemachine.engine

data class Vertex<
    TStateBase : Any,
    TExtendedState : Any,
    TEventBase : Any
    > internal constructor(
    val state: TStateBase,

    val transitions: Map<Class<out TEventBase>, StateTransition<TStateBase, *>>,

    internal val stateProcessor: StateProcessor<TStateBase, TExtendedState, TEventBase>
) {
    suspend fun arrive() = stateProcessor.arrive()
}
