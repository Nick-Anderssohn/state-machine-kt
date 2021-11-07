package com.mrstatemachine.engine

data class Transition<TStateBase : Any>(
    val next: TStateBase
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

        fun transitionTo(nextState: TStateBase) {
            this.nextState = nextState
        }

        fun build() = Transition(nextState)
    }
}
