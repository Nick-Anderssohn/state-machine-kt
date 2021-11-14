package com.mrstatemachine.engine

class Vertex<TStateBase : Any, TEventBase : Any>(
    val state: TStateBase,

    /**
     * key = event
     * value = { map where key=nextState and value=transitionToNextState }
     */
    val transitions: Map<TEventBase, Map<TStateBase, Transition<TStateBase, *>>>,

    val typeBasedEventTransitions: Map<Class<out TEventBase>, Map<TStateBase, Transition<TStateBase, *>>>
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
        private val transitions = mutableMapOf<TEventBase, Map<TStateBase, Transition<TStateBase, *>>>()

        private val typeBasedEventTransitions = mutableMapOf<Class<out TEventBase>, Map<TStateBase, Transition<TStateBase, *>>>()

        inline fun <reified TEvent : TEventBase> on(noinline fn: TransitionsBuilder<TStateBase, TEvent>.() -> Unit) {
            on(TEvent::class.java, fn)
        }

        @PublishedApi
        internal fun <TEvent : TEventBase> on(clazz: Class<TEvent>, fn: TransitionsBuilder<TStateBase, TEvent>.() -> Unit) {
            require(clazz !in typeBasedEventTransitions) { "you may only register each event once per state" }
            typeBasedEventTransitions[clazz] = buildTransitionsByState(fn)
        }

        fun <TEvent : TEventBase> on(event: TEvent, fn: TransitionsBuilder<TStateBase, TEvent>.() -> Unit) {
            require(event !in transitions) { "you may only register each event once per state" }
            transitions[event] = buildTransitionsByState(fn)
        }

        fun build() = Vertex<TStateBase, TEventBase>(
            state = state,
            transitions = transitions,
            typeBasedEventTransitions = typeBasedEventTransitions
        )

        private fun <TEvent : TEventBase> buildTransitionsByState(
            fn: TransitionsBuilder<TStateBase, TEvent>.() -> Unit
        ): Map<TStateBase, Transition<TStateBase, TEvent>> {
            val newTransitions = TransitionsBuilder<TStateBase, TEvent>()
                .apply { fn() }
                .build()

            val newTransitionsByState = mutableMapOf<TStateBase, Transition<TStateBase, TEvent>>()

            for (transition in newTransitions) {
                require(transition.next !in newTransitionsByState) {
                    "each parallel transition must point to a different state"
                }

                newTransitionsByState[transition.next] = transition
            }

            return newTransitionsByState
        }
    }
}
