package com.mrstatemachine.engine

class ExtendedStateStore<TExtendedState : Any>internal constructor() {
    @Volatile
    internal lateinit var _extendedState: TExtendedState

    val extendedState get() = _extendedState
}
