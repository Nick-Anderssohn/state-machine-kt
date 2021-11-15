package com.mrstatemachine.engine

import com.mrstatemachine.StateProcessor

class Vertex<TStateBase : Any, TEventBase : Any>(
    val state: TStateBase,

    val transitions: Map<TEventBase, StateTransition<TStateBase, *>>,

    val typeBasedEventTransitions: Map<Class<out TEventBase>, StateTransition<TStateBase, *>>,

    val stateProcessor: StateProcessor<TStateBase>?
) {
    companion object {
        operator fun <TStateBase : Any, TEventBase : Any> invoke(
            state: TStateBase,
            fn: Builder<TStateBase, TEventBase>.() -> Unit
        ): Vertex<TStateBase, TEventBase> {
            val builder = Builder<TStateBase, TEventBase>(state)
            builder.fn()
            return builder.build()
        }
    }

    class Builder<TStateBase : Any, TEventBase : Any>(
        private val state: TStateBase
    ) {
        private val transitions = mutableMapOf<TEventBase, StateTransition<TStateBase, *>>()

        private val typeBasedEventTransitions = mutableMapOf<Class<out TEventBase>, StateTransition<TStateBase, *>>()

        private var arrivalBuildData: ArrivalBuilder.BuildData<TStateBase>? = null

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

        fun uponArrival(fn: ArrivalBuilder<TStateBase>.() -> Unit) {
            val builder = ArrivalBuilder<TStateBase>()
            builder.fn()
            arrivalBuildData = builder.build()
        }

        fun build() = Vertex<TStateBase, TEventBase>(
            state = state,
            transitions = transitions,
            typeBasedEventTransitions = typeBasedEventTransitions,
            stateProcessor = StateProcessor(
                onArrival = arrivalBuildData?.onArrival,
                postArrivalNextState = arrivalBuildData?.postArrivalNextState
            )
        )

        private fun <TEvent : TEventBase> buildTransition(
            fn: TransitionsBuilder<TStateBase, TEvent>.() -> Unit
        ) = TransitionsBuilder<TStateBase, TEvent>()
            .apply { fn() }
            .buildStateTransition()
    }

    class ArrivalBuilder<TStateBase : Any> {
        private val result = BuildData<TStateBase>()

        fun run(fn: suspend () -> Unit) {
            this.result.onArrival = fn
        }

        fun afterRunTransitionTo(next: TStateBase?) {
            this.result.postArrivalNextState = next
        }

        fun build() = result

        data class BuildData<TStateBase : Any>(
            var postArrivalNextState: TStateBase? = null,
            var onArrival: (suspend () -> Unit)? = null
        )
    }
}
