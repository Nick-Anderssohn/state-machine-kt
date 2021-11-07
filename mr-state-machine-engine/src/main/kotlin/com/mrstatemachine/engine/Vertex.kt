package com.mrstatemachine.engine

data class Vertex<TEvent : Any, TState : Any>(
    val state: TState,

    // Todo: make immutable
    val transitions: MutableMap<TEvent, Transition<TEvent, TState>> = mutableMapOf()
)
