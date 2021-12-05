package com.mrstatemachine.engine

import com.mrstatemachine.TransitionTask

/**
 * Transition pointing to a state
 */
class StateTransition<TStateBase : Any, in TEvent : Any> internal constructor(
    val next: TStateBase,
    val task: TransitionTask<TEvent>?
)
