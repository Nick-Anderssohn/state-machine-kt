package com.mrstatemachine.engine

import com.mrstatemachine.TransitionTask

class StateMachine<TStateBase : Any, TExtendedState : Any, TEventBase : Any> private constructor(
    internal val stateStore: StateStore<TStateBase, TExtendedState>,
    private val vertices: Map<TStateBase, Vertex<TStateBase, TExtendedState, TEventBase, *, *>>
) {
    companion object {
        operator fun <TStateBase : Any, TExtendedState : Any, TEventBase : Any> invoke(
            fn: Builder<TStateBase, TExtendedState, TEventBase>.() -> Unit
        ): StateMachine<TStateBase, TExtendedState, TEventBase> {
            val builder = Builder<TStateBase, TExtendedState, TEventBase>()
            builder.fn()
            return builder.build()
        }
    }

    @Volatile
    var currentVertex: Vertex<TStateBase, TExtendedState, TEventBase, *, *> = requireNotNull(vertices[stateStore.currentState]) {
        "no configuration exists for starting state"
    }

    // Todo: What are we going to do with failures?
    suspend fun <TEvent : TEventBase> processEvent(event: TEvent) {
        val transition = currentVertex.transitions[event::class.java]
            ?: return

        val nextVertex = vertices[transition.next]

        if (transition.task != null) {
            (transition.task as TransitionTask<TEvent>).run(event)
        }

        currentVertex = nextVertex!!

        val newExtendedState = currentVertex.stateProcessor.arrive(stateStore.extendedState)

        if (newExtendedState != null) {
            stateStore.extendedState = newExtendedState
        }

        if (event::class.java in currentVertex.stateProcessor.eventsToPropagate) {
            processEvent(event)
        }
    }

    class Builder<TStateBase : Any, TExtendedState : Any, TEventBase : Any> {
        private lateinit var acceptingState: TStateBase
        private var acceptingExtendedState: TExtendedState? = null
        private val vertices: MutableMap<TStateBase, Vertex<TStateBase, TExtendedState, TEventBase, *, *>> = mutableMapOf()

        fun startingState(value: TStateBase) {
            require(!this::acceptingState.isInitialized) {
                "state machine can only have one starting state"
            }

            this.acceptingState = value
        }

        fun startingExtendedState(value: TExtendedState) {
            require(acceptingExtendedState == null) {
                "state machine can only have one starting extended state"
            }

            this.acceptingExtendedState = value
        }

        fun <TArrivalInput : Any, TArrivalOutput : Any> state(
            state: TStateBase,
            fn: Vertex.Builder<TStateBase, TExtendedState, TEventBase, TArrivalInput, TArrivalOutput>.() -> Unit
        ) {
            require(state !in vertices) {
                "each state may only be defined once per state machine"
            }

            vertices[state] = Vertex(state, fn)
        }

        fun build() = StateMachine<TStateBase, TExtendedState, TEventBase>(
            StateStore(
                currentState = acceptingState,
                extendedState = acceptingExtendedState
            ),
            vertices
        )
    }
}
