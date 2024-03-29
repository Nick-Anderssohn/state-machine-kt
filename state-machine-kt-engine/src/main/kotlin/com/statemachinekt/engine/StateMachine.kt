package com.statemachinekt.engine

import com.statemachinekt.dsl.StateMachineBuilder
import java.lang.IllegalArgumentException

class StateMachine<TStateBase : Any, TExtendedState : Any, TEventBase : Any> internal constructor (
    private val stateStore: StateStore<TStateBase, TExtendedState>,
    private val vertices: Map<TStateBase, Vertex<TStateBase, TExtendedState, TEventBase>>,
    private val superVertex: Vertex<TStateBase, TExtendedState, TEventBase>,
    private val config: StateMachineConfig
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
    private var currentVertex: Vertex<TStateBase, TExtendedState, TEventBase> = requireNotNull(vertices[stateStore.acceptingState]) {
        "no configuration exists for starting state"
    }

    // Todo: What are we going to do with failures?
    suspend fun <TEvent : TEventBase> processEvent(event: TEvent) {
        val transition = currentVertex.transitions[event::class.java]
            ?: superVertex.transitions[event::class.java]
            ?: return handleUnknownEvent(event)

        val transitionResult = transition.task?.let {
            @Suppress("UNCHECKED_CAST")
            (transition.task as TransitionTask<TEvent, TStateBase, TExtendedState>).invoke(event, stateStore.extendedStateStore)
        }

        stateStore.applyIf(transitionResult != null) {
            this.extendedStateStore.extState = transitionResult!!.extendedState
        }

        // Run exit action if we are transitioning to a different state
        if (transitionResult?.nextState != null && transitionResult.nextState != currentState) {
            currentVertex.exit(event)
        }

        currentVertex = vertices[transitionResult?.nextState ?: transition.next ?: currentState]!!

        val result = currentVertex.arrive(event)
            ?: superVertex.arrive(event)
            ?: ActionResult(stateStore.extendedStateStore.extendedState)

        stateStore.extendedStateStore.extState = result.extendedState

        result.eventToTrigger?.let { processEvent(it) }
    }

    // TODO: Actually, make this configurable at the vertex level too.
    private fun <TEvent : TEventBase> handleUnknownEvent(event: TEvent) {
        if (config.throwExceptionOnUnrecognizedEvent) {
            throw IllegalArgumentException("cannot handle event $event")
        }
    }
}
