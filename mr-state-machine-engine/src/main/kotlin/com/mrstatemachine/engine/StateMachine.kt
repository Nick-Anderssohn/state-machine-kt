package com.mrstatemachine.engine

import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.concurrent.ConcurrentHashMap

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

    internal val currentVertices = ConcurrentHashMap.newKeySet<Vertex<TStateBase, TEventBase>>()
        .apply { this += vertices[startingState] }

    // Todo: What are we going to do with failures?
    suspend fun processEvent(event: TEventBase) = supervisorScope {
        for (vertex in currentVertices) {
            launch {
                val transition = vertex.transitions[event] ?: return@launch

                val nextVertex = vertices[transition.next]

                transition.task?.run()

                currentVertices -= vertex
                currentVertices += nextVertex
            }
        }
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
