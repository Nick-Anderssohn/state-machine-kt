package com.mrstatemachine.engine

fun interface Action<TExtendedState : Any> {
    suspend fun invoke(extendedStateStore: ExtendedStateStore<TExtendedState>): TExtendedState
}
