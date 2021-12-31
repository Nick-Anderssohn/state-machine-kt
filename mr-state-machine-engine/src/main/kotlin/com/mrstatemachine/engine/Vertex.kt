package com.mrstatemachine.engine

internal data class Vertex<TStateBase : Any, TExtendedState : Any, TEventBase : Any> internal constructor(
    internal val state: TStateBase?,
    internal val transitions: Map<Class<out TEventBase>, StateTransition<TStateBase, TExtendedState, *>>,
    private val stateStore: StateStore<TStateBase, TExtendedState>,
    private val onArrival: Action<TEventBase, TExtendedState>? = null
) {
    internal suspend fun arrive(event: TEventBase): ActionResult<TEventBase, TExtendedState>? {
        return onArrival?.invoke(event, stateStore.extendedStateStore)
    }
}
