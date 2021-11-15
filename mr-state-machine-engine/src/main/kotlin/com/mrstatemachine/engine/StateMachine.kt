package com.mrstatemachine.engine

import com.mrstatemachine.TransitionTask

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

    @Volatile
    var currentVertex: Vertex<TStateBase, TEventBase> = requireNotNull(vertices[startingState]) {
        "no configuration exists for starting state"
    }

    // Todo: What are we going to do with failures?
    suspend fun <TEvent : TEventBase> processEvent(event: TEvent) {
        val transition = currentVertex.transitions[event]
            ?: currentVertex.typeBasedEventTransitions[event::class.java]
            ?: return

        val nextVertex = vertices[transition.next]

        if (transition.task != null) {
            (transition.task as TransitionTask<TEvent>).run(event)
        }

        currentVertex = nextVertex!!

        nextVertex.stateProcessor?.onArrival?.invoke()

        // Todo: Should we do something if none of the current vertices can handle the provided event?
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
            require(state !in vertices) {
                "each state may only be defined once"
            }

            vertices[state] = Vertex(state, fn)
        }

        fun build() = StateMachine<TStateBase, TEventBase>(acceptingState, vertices)
    }
}
