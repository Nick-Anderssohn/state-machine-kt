package com.mrstatemachine.engine

class ExtendedStateStore<TExtendedState : Any>internal constructor(
    @Volatile
    internal var extState: TExtendedState
) {
    val extendedState get() = extState
}
