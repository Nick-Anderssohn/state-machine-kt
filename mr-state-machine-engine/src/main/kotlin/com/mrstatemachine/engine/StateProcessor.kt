package com.mrstatemachine.engine

internal class StateProcessor<TStateBase : Any, TExtendedState : Any, TEventBase : Any>internal constructor(
    private val stateStore: StateStore<TStateBase, TExtendedState>,
    private val onArrival: Action<TEventBase, TExtendedState>? = null
) {
    internal suspend fun arrive(event: TEventBase): ActionResult<TEventBase, TExtendedState>? {
        return onArrival?.invoke(event, stateStore.extendedStateStore)
    }
}
