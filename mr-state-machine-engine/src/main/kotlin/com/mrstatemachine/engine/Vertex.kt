package com.mrstatemachine.engine

data class Vertex<TStateBase : Any, TExtendedState : Any, TEventBase : Any> internal constructor(
    val state: TStateBase?,
    val transitions: Map<Class<out TEventBase>, StateTransition<TStateBase, TExtendedState, *>>,
    private val stateStore: StateStore<TStateBase, TExtendedState>,
    private val onArrival: Action<TEventBase, TExtendedState>? = null
) {
    internal suspend fun arrive(event: TEventBase): ActionResult<TEventBase, TExtendedState>? {
        return onArrival?.invoke(event, stateStore.extendedStateStore)
    }
}
