package com.mrstatemachine.engine

class Vertex<
    TStateBase : Any,
    TExtendedState : Any,
    TEventBase : Any,
    TArrivalInput : Any,
    TArrivalOutput : Any
    > internal constructor(
    val state: TStateBase,

    val transitions: Map<Class<out TEventBase>, StateTransition<TStateBase, *>>,

    val stateProcessor: StateProcessor<TStateBase, TExtendedState, TEventBase, TArrivalInput, TArrivalOutput>
)
