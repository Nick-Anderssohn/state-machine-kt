package com.mrstatemachine.engine

data class ActionResult<TEventBase : Any, TExtendedState : Any>(
    /**
     * The new extended state.
     */
    val extendedState: TExtendedState,

    /**
     * Optional. If you want to automatically trigger another event
     * after the arrival action returns, then you can stick that
     * event here.
     */
    val eventToTrigger: TEventBase? = null
)

fun interface Action<TEventBase : Any, TExtendedState : Any> {
    suspend fun invoke(
        event: TEventBase,
        extendedStateStore: ExtendedStateStore<TExtendedState>
    ): ActionResult<TEventBase, TExtendedState>
}
