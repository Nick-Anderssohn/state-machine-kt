package com.mrstatemachine

fun interface TransitionTask<in TEvent : Any> {
    suspend fun run(event: TEvent): Unit
}

class StateProcessor<TStateBase : Any>(
    val onArrival: (suspend () -> Unit)? = null,
    val postArrivalNextState: TStateBase? = null
)
