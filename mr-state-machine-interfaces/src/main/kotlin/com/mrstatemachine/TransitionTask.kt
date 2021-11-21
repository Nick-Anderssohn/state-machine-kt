package com.mrstatemachine

fun interface TransitionTask<in TEvent : Any> {
    suspend fun run(event: TEvent): Unit
}
