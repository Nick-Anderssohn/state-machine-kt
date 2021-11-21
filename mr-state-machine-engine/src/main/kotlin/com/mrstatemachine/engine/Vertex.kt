package com.mrstatemachine.engine

import com.mrstatemachine.Extractor
import com.mrstatemachine.Merger
import com.mrstatemachine.NoOpMerger

class Vertex<
    TStateBase : Any,
    TExtendedState : Any,
    TEventBase : Any,
    TArrivalInput : Any,
    TArrivalOutput : Any
    >(
    val state: TStateBase,

    val transitions: Map<TEventBase, StateTransition<TStateBase, *>>,

    val typeBasedEventTransitions: Map<Class<out TEventBase>, StateTransition<TStateBase, *>>,

    val stateProcessor: StateProcessor<TStateBase, TExtendedState, TEventBase, TArrivalInput, TArrivalOutput>
) {
    companion object {
        operator fun <
            TStateBase : Any,
            TEventBase : Any,
            TExtendedState : Any,
            TArrivalInput : Any,
            TArrivalOutput : Any
            > invoke(
            state: TStateBase,
            fn: Builder<TStateBase, TExtendedState, TEventBase, TArrivalInput, TArrivalOutput>.() -> Unit
        ): Vertex<TStateBase, TExtendedState, TEventBase, TArrivalInput, TArrivalOutput> {
            val builder = Builder<TStateBase, TExtendedState, TEventBase, TArrivalInput, TArrivalOutput>(state)
            builder.fn()
            return builder.build()
        }
    }

    class Builder<
        TStateBase : Any,
        TExtendedState : Any,
        TEventBase : Any,
        TArrivalInput : Any,
        TArrivalOutput : Any
        >(
        private val state: TStateBase
    ) {
        private val transitions = mutableMapOf<TEventBase, StateTransition<TStateBase, *>>()

        private val typeBasedEventTransitions = mutableMapOf<Class<out TEventBase>, StateTransition<TStateBase, *>>()

        private var arrivalBuildData: ArrivalBuilder.BuildData<TStateBase, TExtendedState, TEventBase, TArrivalInput, TArrivalOutput>? = null

        inline fun <reified TEvent : TEventBase> on(noinline fn: TransitionsBuilder<TStateBase, TEvent>.() -> Unit) {
            on(TEvent::class.java, fn)
        }

        @PublishedApi
        internal fun <TEvent : TEventBase> on(clazz: Class<TEvent>, fn: TransitionsBuilder<TStateBase, TEvent>.() -> Unit) {
            require(clazz !in typeBasedEventTransitions) { "you may only register each event once per state" }
            typeBasedEventTransitions[clazz] = buildTransition(fn)
        }

        fun <TEvent : TEventBase> on(event: TEvent, fn: TransitionsBuilder<TStateBase, TEvent>.() -> Unit) {
            require(event !in transitions) { "you may only register each event once per state" }
            transitions[event] = buildTransition(fn)
        }

        fun uponArrival(fn: ArrivalBuilder<TStateBase, TExtendedState, TEventBase, TArrivalInput, TArrivalOutput>.() -> Unit) {
            val builder = ArrivalBuilder<TStateBase, TExtendedState, TEventBase, TArrivalInput, TArrivalOutput>()
            builder.fn()
            arrivalBuildData = builder.build()
        }

        fun build() = Vertex<TStateBase, TExtendedState, TEventBase, TArrivalInput, TArrivalOutput>(
            state = state,
            transitions = transitions,
            typeBasedEventTransitions = typeBasedEventTransitions,
            stateProcessor = StateProcessor(
                onArrival = arrivalBuildData?.onArrival,
                eventsToPropagate = arrivalBuildData?.eventsToPropagate ?: emptySet(),
                merger = arrivalBuildData?.merger ?: NoOpMerger(),
                extractor = arrivalBuildData?.extractor
            )
        )

        private fun <TEvent : TEventBase> buildTransition(
            fn: TransitionsBuilder<TStateBase, TEvent>.() -> Unit
        ) = TransitionsBuilder<TStateBase, TEvent>()
            .apply { fn() }
            .buildStateTransition()
    }

    class ArrivalBuilder<TStateBase : Any, TExtendedState : Any, TEventBase : Any, TInput : Any, TOutput : Any> {
        private val result = BuildData<TStateBase, TExtendedState, TEventBase, TInput, TOutput>()

        fun execute(fn: suspend (input: TInput) -> TOutput) {
            this.result.onArrival = fn
        }

        inline fun <reified TEvent : TEventBase> propagateEvent() {
            propagateEvent(TEvent::class.java)
        }

        @PublishedApi
        internal fun <TEvent : TEventBase> propagateEvent(clazz: Class<TEvent>) {
            this.result.eventsToPropagate.add(clazz)
        }

        // todo: something more dsl-like?
        fun storeExecutionOutput(merger: Merger<TOutput, TExtendedState>?) {
            result.merger = merger
        }

        // todo: something more dsl-like?
        fun extractInputFromExtendedState(extractor: Extractor<TInput, TExtendedState>?) {
            result.extractor = extractor
        }

        fun build() = result

        data class BuildData<TStateBase : Any, TExtendedState : Any, TEventBase : Any, TInput : Any, TOutput : Any,>(
            var eventsToPropagate: MutableSet<Class<out TEventBase>> = mutableSetOf(),
            var onArrival: (suspend (input: TInput) -> TOutput)? = null,
            var merger: Merger<TOutput, TExtendedState>? = null,
            var extractor: Extractor<TInput, TExtendedState>? = null
        )
    }
}
