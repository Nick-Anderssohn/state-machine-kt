package com.mrstatemachine.engine

import com.mrstatemachine.dsl.StateMachineBuilder

class StateMachine<TStateBase : Any, TExtendedState : Any, TEventBase : Any> internal constructor(
    internal val stateStore: StateStore<TStateBase, TExtendedState>,
    private val vertices: Map<TStateBase, Vertex<TStateBase, TExtendedState, TEventBase>>,
) {
    companion object {
        /**
         * As a general rule-of-thumb, you don't want to create a circular imports
         * between packages, but this is an okay exception to that rule. Builders
         * are frequently declared as inner classes anyways; it's just nice
         * organization-wise to separate out the DSL into its own package.
         */
        operator fun <TStateBase : Any, TExtendedState : Any, TEventBase : Any> invoke(
            fn: StateMachineBuilder<TStateBase, TExtendedState, TEventBase>.() -> Unit
        ): StateMachine<TStateBase, TExtendedState, TEventBase> {
            val builder = StateMachineBuilder<TStateBase, TExtendedState, TEventBase>()
            builder.fn()
            return builder.build()
        }
    }

    @Volatile
    var currentVertex: Vertex<TStateBase, TExtendedState, TEventBase> = requireNotNull(vertices[stateStore.currentState]) {
        "no configuration exists for starting state"
    }

    // Todo: What are we going to do with failures?
    suspend fun <TEvent : TEventBase> processEvent(event: TEvent) {
        val transition = currentVertex.transitions[event::class.java]
            ?: return

        val nextVertex = vertices[transition.next]

        if (transition.task != null) {
            @Suppress("UNCHECKED_CAST")
            val newExtendedState = (transition.task as TransitionTask<TEvent, TExtendedState>)
                .invoke(event, stateStore.extendedStateStore)

            stateStore.extendedStateStore._extendedState = newExtendedState
        }

        currentVertex = nextVertex!!

        currentVertex.arrive(event)

        if (event::class.java in currentVertex.stateProcessor.eventsToPropagate) {
            processEvent(event)
        }
    }
}
