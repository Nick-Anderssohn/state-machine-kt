package com.mrstatemachine.engine

data class TransitionTaskResult<TStateBase : Any, TExtendedState : Any>(
    /**
     * The new extended state.
     */
    val extendedState: TExtendedState,

    /**
     * The next state. This will be preferred over any default configured
     * for this transition at the state machine level.
     */
    val nextState: TStateBase? = null
)

fun interface TransitionTask<in TEvent : Any, TStateBase : Any, TExtendedState : Any> {
    suspend fun invoke(
        event: TEvent,
        extendedStateStore: ExtendedStateStore<TExtendedState>
    ): TransitionTaskResult<TStateBase, TExtendedState>
}
