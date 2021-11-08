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

    suspend fun processEvent(event: TEventBase) {
        val transition = requireNotNull(currentVertex.transitions[event]) {
            "provided event cannot be handled by current vertex"
        }

        val nextVertex = requireNotNull(vertices[transition.next]) { "state ${currentVertex.state} does not support event $event" }

        val taskResult = transition.task?.run()

        currentVertex = nextVertex
    }

    class Builder<TStateBase : Any, TEventBase : Any> {
        private lateinit var acceptingState: TStateBase
        private val vertices: MutableMap<TStateBase, Vertex<TStateBase, TEventBase>> = mutableMapOf()

        fun startingState(value: TStateBase) {
            require(!this::acceptingState.isInitialized) {
                "state machine can only have one starting state!"
            }

            this.acceptingState = value
        }

        fun state(state: TStateBase, fn: Vertex.Builder<TStateBase, TEventBase>.() -> Unit) {
            vertices[state] = Vertex(state, fn)
        }

        fun build() = StateMachine<TStateBase, TEventBase>(acceptingState, vertices)
    }
}
