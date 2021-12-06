package com.mrstatemachine.engine

internal data class StateStore<TStateBase : Any, TExtendedState : Any>(
    @Volatile
    var currentState: TStateBase,

    @Volatile
    var extendedState: TExtendedState?,

    val vertexToMostRecentOutput: MutableMap<Vertex<TStateBase, TExtendedState, *, *, *>, Any?> =
        mutableMapOf<Vertex<TStateBase, TExtendedState, *, *, *>, Any?>()
)
