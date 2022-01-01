package com.mrstatemachine.engine

data class StateMachineConfig(
    /**
     * Whether to throw an exception if the current state
     * doesn't know how to handle an incoming event. If false, then
     * it would just be a no-op. This is false by default.
     */
    val throwExceptionOnUnrecognizedEvent: Boolean = false
)
