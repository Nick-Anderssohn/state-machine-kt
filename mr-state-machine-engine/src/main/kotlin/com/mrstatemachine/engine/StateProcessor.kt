package com.mrstatemachine.engine

internal class StateProcessor<TStateBase : Any, TExtendedState : Any, in TEventBase : Any>internal constructor(
    private val stateStore: StateStore<TStateBase, TExtendedState>,
    private val onArrival: Action<TEventBase, TExtendedState>? = null,
    val eventsToPropagate: Set<Class<*>> = emptySet()
) {
    internal suspend fun arrive(event: TEventBase): TExtendedState? {
        return onArrival?.invoke(event, stateStore.extendedStateStore)
    }
}
