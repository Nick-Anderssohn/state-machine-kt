package com.mrstatemachine.engine

data class Transition<TEvent : Any, TState : Any>(
    val next: Vertex<TEvent, TState>
)
