package com.mrstatemachine.engine

import com.mrstatemachine.Task

interface TransitionBuilder<TStateBase : Any> {
    fun transitionTo(nextState: TStateBase)
    fun execute(task: Task?)
}

class Transition<TStateBase : Any> private constructor(
    val next: TStateBase,
    val task: Task?
) {
    companion object {
        operator fun <TStateBase : Any> invoke(
            fn: Builder<TStateBase>.() -> Unit
        ): Transition<TStateBase> {
            val builder = Builder<TStateBase>()
            builder.fn()
            return builder.build()
        }
    }

    class Builder<TStateBase : Any> : TransitionBuilder<TStateBase> {
        internal var isInitialized = false
        internal lateinit var nextState: TStateBase

        private var task: Task? = null

        override fun transitionTo(nextState: TStateBase) = builderFn {
            require(!this::nextState.isInitialized) {
                "next state is already configured to $nextState"
            }

            this.nextState = nextState
        }

        override fun execute(task: Task?) = builderFn {
            require(this.task == null) {
                "there can only be one task per transition"
            }

            this.task = task
        }

        internal fun build() = Transition(
            next = nextState,
            task = task
        )

        private fun builderFn(fn: Builder<TStateBase>.() -> Unit) {
            isInitialized = true
            this.fn()
        }
    }
}

class TransitionsBuilder<TStateBase : Any>internal constructor(
    private val singleTransitionBuilder: Transition.Builder<TStateBase> = Transition.Builder<TStateBase>()
) : TransitionBuilder<TStateBase> by singleTransitionBuilder {
    private val parallelTransitions: MutableList<Transition.Builder<TStateBase>.() -> Unit> = mutableListOf()

    fun inParallel(fn: Transition.Builder<TStateBase>.() -> Unit) {
        parallelTransitions += fn
    }

    internal fun build(): List<Transition<TStateBase>> {
        val transitions = parallelTransitions.map { Transition<TStateBase>(it) }

        if (singleTransitionBuilder.isInitialized) {
            return transitions + singleTransitionBuilder.build()
        }

        return transitions
    }
}
