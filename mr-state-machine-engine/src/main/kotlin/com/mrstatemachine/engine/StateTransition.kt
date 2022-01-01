package com.mrstatemachine.engine

/**
 * Transition pointing to a state
 */
data class StateTransition<TStateBase : Any, TExtendedState : Any, in TEvent : Any> internal constructor(
    val next: TStateBase?,
    val task: TransitionTask<TEvent, TStateBase, TExtendedState>?
)
