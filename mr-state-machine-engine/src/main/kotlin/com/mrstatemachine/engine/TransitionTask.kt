package com.mrstatemachine.engine

fun interface TransitionTask<in TEvent : Any, TExtendedState : Any> {
    suspend fun invoke(event: TEvent, extendedStateStore: ExtendedStateStore<TExtendedState>): TExtendedState
}
