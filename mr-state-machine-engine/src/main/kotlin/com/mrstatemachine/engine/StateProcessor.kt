package com.mrstatemachine.engine

internal class StateProcessor<TStateBase : Any, TExtendedState : Any, in TEventBase : Any>internal constructor(
    private val stateStore: StateStore<TStateBase, TExtendedState>,
    private val onArrival: Action<TEventBase, TExtendedState>? = null,
    val eventsToPropagate: Set<Class<*>> = emptySet()
) {
    internal suspend fun arrive(event: TEventBase) {
        if (onArrival == null) {
            return
        }

        stateStore.extendedStateStore._extendedState = onArrival.invoke(event, stateStore.extendedStateStore)
    }
}
