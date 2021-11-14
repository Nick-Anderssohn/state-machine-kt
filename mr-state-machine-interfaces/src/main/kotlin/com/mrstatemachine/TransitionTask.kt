package com.mrstatemachine

interface TransitionTask<in TEvent : Any> {
    suspend fun run(event: TEvent): Unit
}
