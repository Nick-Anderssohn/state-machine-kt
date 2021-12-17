package com.mrstatemachine.engine

internal class StateProcessor<TStateBase : Any, TExtendedState : Any, TEventBase : Any>internal constructor(
    private val stateStore: StateStore<TStateBase, TExtendedState>,
    private val onArrival: Action<TExtendedState>? = null,
    val eventsToPropagate: Set<Class<out TEventBase>> = emptySet()
) {
    internal suspend fun arrive() {
        if (onArrival == null) {
            return
        }

        stateStore.extendedStateStore._extendedState = onArrival.invoke(stateStore.extendedStateStore)
    }
}
