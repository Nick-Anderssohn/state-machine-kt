package com.mrstatemachine.engine

import com.mrstatemachine.Task

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

    class Builder<TStateBase : Any> {
        private lateinit var nextState: TStateBase
        private var task: Task? = null

        fun transitionTo(nextState: TStateBase) {
            require(!this::nextState.isInitialized) {
                "next state is already configured to $nextState"
            }

            this.nextState = nextState
        }

        fun execute(task: Task?) {
            require(this.task == null) {
                "there can only be one task per transition"
            }

            this.task = task
        }

        fun build() = Transition(
            next = nextState,
            task = task
        )
    }
}
