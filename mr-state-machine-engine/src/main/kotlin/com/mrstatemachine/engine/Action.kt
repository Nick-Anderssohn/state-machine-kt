package com.mrstatemachine.engine

fun interface Action<in TEventBase : Any, TExtendedState : Any> {
    suspend fun invoke(event: TEventBase, extendedStateStore: ExtendedStateStore<TExtendedState>): TExtendedState
}
