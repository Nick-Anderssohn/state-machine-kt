package com.mrstatemachine.engine

import com.mrstatemachine.TransitionTask

interface TransitionBuilder<TStateBase : Any, TEvent : Any> {
    fun transitionTo(nextState: TStateBase)
    fun execute(task: TransitionTask<TEvent>?)
}

class Transition<TStateBase : Any, in TEvent : Any> private constructor(
    val next: TStateBase,
    val task: TransitionTask<TEvent>?
) {
    companion object {
        operator fun <TStateBase : Any, TEvent : Any> invoke(
            fn: Builder<TStateBase, TEvent>.() -> Unit
        ): Transition<TStateBase, TEvent> {
            val builder = Builder<TStateBase, TEvent>()
            builder.fn()
            return builder.build()
        }
    }

    class Builder<TStateBase : Any, TEvent : Any> : TransitionBuilder<TStateBase, TEvent> {
        internal var isInitialized = false
        private lateinit var nextState: TStateBase

        private var task: TransitionTask<TEvent>? = null

        override fun transitionTo(nextState: TStateBase) = builderFn {
            require(!this::nextState.isInitialized) {
                "next state is already configured to $nextState"
            }

            this.nextState = nextState
        }

        override fun execute(task: TransitionTask<TEvent>?) = builderFn {
            require(this.task == null) {
                "there can only be one task per transition"
            }

            this.task = task
        }

        internal fun build() = Transition(
            next = nextState,
            task = task
        )

        private fun builderFn(fn: Builder<TStateBase, TEvent>.() -> Unit) {
            isInitialized = true
            this.fn()
        }
    }
}

class TransitionsBuilder<TStateBase : Any, TEvent : Any>internal constructor(
    private val singleTransitionBuilder: Transition.Builder<TStateBase, TEvent> = Transition.Builder<TStateBase, TEvent>()
) : TransitionBuilder<TStateBase, TEvent> by singleTransitionBuilder {
    private val parallelTransitions: MutableList<Transition.Builder<TStateBase, TEvent>.() -> Unit> = mutableListOf()

    fun inParallel(fn: Transition.Builder<TStateBase, TEvent>.() -> Unit) {
        parallelTransitions += fn
    }

    internal fun build(): List<Transition<TStateBase, TEvent>> {
        val transitions = parallelTransitions.map { Transition<TStateBase, TEvent>(it) }

        if (singleTransitionBuilder.isInitialized) {
            return transitions + singleTransitionBuilder.build()
        }

        return transitions
    }
}
