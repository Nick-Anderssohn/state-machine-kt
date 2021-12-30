package com.mrstatemachine.engine

import com.mrstatemachine.dsl.StateMachineBuilder

class StateMachine<TStateBase : Any, TExtendedState : Any, TEventBase : Any> internal constructor (
    internal val stateStore: StateStore<TStateBase, TExtendedState>,
    private val vertices: Map<TStateBase, Vertex<TStateBase, TExtendedState, TEventBase>>,
    private val superVertex: Vertex<TStateBase, TExtendedState, TEventBase>
) {
    // Will never be null since currentVertex will never be superVertex,
    // which is the only one that is allowed to have a null state.
    val currentState get() = currentVertex.state!!

    val currentExtendedState get() = stateStore.extendedStateStore.extendedState

    companion object {
        /**
         * As a general rule-of-thumb, you don't want to create circular imports
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
    var currentVertex: Vertex<TStateBase, TExtendedState, TEventBase> = requireNotNull(vertices[stateStore.acceptingState]) {
        "no configuration exists for starting state"
    }

    // Todo: What are we going to do with failures?
    suspend fun <TEvent : TEventBase> processEvent(event: TEvent) {
        val transition = currentVertex.transitions[event::class.java]
            ?: superVertex.transitions[event::class.java]
            ?: return

        val nextVertex = vertices[transition.next ?: currentState]

        if (transition.task != null) {
            @Suppress("UNCHECKED_CAST")
            val newExtendedState = (transition.task as TransitionTask<TEvent, TExtendedState>)
                .invoke(event, stateStore.extendedStateStore)

            stateStore.extendedStateStore.extState = newExtendedState
        }

        currentVertex = nextVertex!!

        val result = currentVertex.arrive(event)
            ?: superVertex.arrive(event)
            ?: ActionResult(stateStore.extendedStateStore.extendedState)

        stateStore.extendedStateStore.extState = result.extendedState

        result.eventToTrigger?.let { processEvent(it) }
    }
}
