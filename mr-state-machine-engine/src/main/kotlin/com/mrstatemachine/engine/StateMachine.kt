package com.mrstatemachine.engine

class StateMachine<TEvent : Any, TState : Any>(
    private val acceptingState: Vertex<TEvent, TState>
) {
    var currentVertex = acceptingState
        private set

    fun processEvent(event: TEvent) {
        val transition = currentVertex.transitions[event]
            ?: throw IllegalArgumentException("provided event cannot be handled by current vertex")

        currentVertex = transition.next
    }
}
