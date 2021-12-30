package com.mrstatemachine.engine

class ExtendedStateStore<TExtendedState : Any>internal constructor() {
    @Volatile
    internal lateinit var extState: TExtendedState

    val extendedState get() = extState
}
