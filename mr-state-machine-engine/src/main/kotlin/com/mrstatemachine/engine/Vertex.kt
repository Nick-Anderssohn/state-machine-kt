package com.mrstatemachine.engine

class Vertex<TStateBase : Any, TEventBase : Any>(
    val state: TStateBase,
    val transitions: Map<TEventBase, Transition<TStateBase>>
) {
    companion object {
        operator fun <TStateBase : Any, TEventBase : Any> invoke(
            state: TStateBase,
            fn: Builder<TStateBase, TEventBase>.() -> Unit
        ): Vertex<TStateBase, TEventBase> {
            val builder = Builder<TStateBase, TEventBase>(state)
            builder.fn()
            return builder.build()
        }
    }

    class Builder<TStateBase : Any, TEventBase : Any>(
        private val state: TStateBase
    ) {
        private val transitions: MutableMap<TEventBase, Transition<TStateBase>> = mutableMapOf()

        fun on(event: TEventBase, fn: Transition.Builder<TStateBase>.() -> Unit) {
            require(event !in transitions) { "you may only register each event once per state" }

            transitions[event] = Transition<TStateBase>(fn)
        }

        fun build() = Vertex<TStateBase, TEventBase>(state, transitions)
    }
}
