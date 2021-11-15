package com.mrstatemachine.engine

import com.mrstatemachine.TransitionTask

interface StateTransitionBuilder<TStateBase : Any, TEvent : Any> {
    fun transitionTo(nextState: TStateBase)
    fun execute(task: TransitionTask<TEvent>?)
}

/**
 * Transition pointing to a state
 */
class StateTransition<TStateBase : Any, in TEvent : Any> private constructor(
    val next: TStateBase,
    val task: TransitionTask<TEvent>?
) {
    companion object {
        operator fun <TStateBase : Any, TEvent : Any> invoke(
            fn: Builder<TStateBase, TEvent>.() -> Unit
        ): StateTransition<TStateBase, TEvent> {
            val builder = Builder<TStateBase, TEvent>()
            builder.fn()
            return builder.build()
        }
    }

    class Builder<TStateBase : Any, TEvent : Any> : StateTransitionBuilder<TStateBase, TEvent> {
        private var nextState: TStateBase? = null

        private var task: TransitionTask<TEvent>? = null

        override fun transitionTo(nextState: TStateBase) {
            require(this.nextState == null) {
                "next state is already configured to $nextState"
            }

            this.nextState = nextState
        }

        override fun execute(task: TransitionTask<TEvent>?) {
            require(this.task == null) {
                "there can only be one task per transition"
            }

            this.task = task
        }

        internal fun build(): StateTransition<TStateBase, TEvent> = StateTransition(
            next = checkNotNull(nextState),
            task = task
        )
    }
}

class TransitionsBuilder<TStateBase : Any, TEvent : Any>internal constructor(
    private val stateTransitionBuilder: StateTransition.Builder<TStateBase, TEvent> =
        StateTransition.Builder<TStateBase, TEvent>()
) : StateTransitionBuilder<TStateBase, TEvent> by stateTransitionBuilder {
    internal fun buildStateTransition(): StateTransition<TStateBase, TEvent> {
        return stateTransitionBuilder.build()
    }
}
