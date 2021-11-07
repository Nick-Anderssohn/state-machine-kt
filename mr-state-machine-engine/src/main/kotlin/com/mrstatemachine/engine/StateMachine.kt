package com.mrstatemachine.engine

class StateMachine<TStateBase : Any, TEventBase : Any> private constructor(
    private val startingState: TStateBase,
    private val vertices: Map<TStateBase, Vertex<TStateBase, TEventBase>>
) {
    companion object {
        operator fun <TStateBase : Any, TEventBase : Any> invoke(
            fn: Builder<TStateBase, TEventBase>.() -> Unit
        ): StateMachine<TStateBase, TEventBase> {
            val builder = Builder<TStateBase, TEventBase>()
            builder.fn()
            return builder.build()
        }
    }

    var currentVertex = requireNotNull(vertices[startingState]) { "unknown accepting state" }
        private set

    fun processEvent(event: TEventBase) {
        val transition = requireNotNull(currentVertex.transitions[event]) {
            "provided event cannot be handled by current vertex"
        }

        currentVertex = requireNotNull(vertices[transition.next]) { "state ${currentVertex.state} does not support event $event" }
    }

    class Builder<TStateBase : Any, TEventBase : Any> {
        private lateinit var acceptingState: TStateBase
        private val vertices: MutableMap<TStateBase, Vertex<TStateBase, TEventBase>> = mutableMapOf()

        fun startingState(value: TStateBase) {
            this.acceptingState = value
        }

        fun state(state: TStateBase, fn: Vertex.Builder<TStateBase, TEventBase>.() -> Unit) {
            vertices[state] = Vertex(state, fn)
        }

        fun build() = StateMachine<TStateBase, TEventBase>(acceptingState, vertices)
    }
}
