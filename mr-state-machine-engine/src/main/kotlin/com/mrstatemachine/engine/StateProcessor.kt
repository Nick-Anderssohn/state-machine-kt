package com.mrstatemachine.engine

import com.mrstatemachine.Extractor
import com.mrstatemachine.Merger

class StateProcessor<TStateBase : Any, TExtendedState : Any, TEventBase : Any, TInput : Any, TOutput : Any>(
    val onArrival: (suspend (input: TInput) -> TOutput)? = null,

    // Todo: default to one that just sticks it in its own place in the extended state
    //  somehow? Might need a separate property in StateStore for storing per vertex outputs.
    //  Maybe not...that would be weird with the extractor pattern...might make a default
    //  extended state class. Or a wrapper around the user defined extended state.
    val merger: Merger<TOutput, TExtendedState>,
    val extractor: Extractor<TInput, TExtendedState>?,

    val eventsToPropagate: Set<Class<out TEventBase>> = emptySet()
) {
    init {
        require(onArrival != null && extractor != null || onArrival == null) {
            "if onArrival is defined then there must also be an extractor defined"
        }
    }

    internal suspend fun arrive(extendedState: TExtendedState?): TExtendedState? {
        if (onArrival == null) {
            return extendedState
        }

        val input = extractor!!.extract(extendedState)
        val output = onArrival.invoke(input)

        return merger.merge(output, extendedState)
    }
}
