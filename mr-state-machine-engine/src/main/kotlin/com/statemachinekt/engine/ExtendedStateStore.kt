package com.statemachinekt.engine

class ExtendedStateStore<TExtendedState : Any>internal constructor(
    @Volatile
    internal var extState: TExtendedState
) {
    val extendedState get() = extState
}
