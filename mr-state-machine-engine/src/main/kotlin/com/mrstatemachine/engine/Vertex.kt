package com.mrstatemachine.engine

class Vertex<TStateBase : Any, TEventBase : Any>(
    val state: TStateBase,

    /**
     * key = event
     * value = { map where key=nextState and value=transitionToNextState }
     */
    val transitions: Map<TEventBase, Map<TStateBase, Transition<TStateBase>>>
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
        private val transitions: MutableMap<TEventBase, Map<TStateBase, Transition<TStateBase>>> = mutableMapOf()

        fun on(event: TEventBase, fn: TransitionsBuilder<TStateBase>.() -> Unit) {
            require(event !in transitions) { "you may only register each event once per state" }

            val newTransitions = TransitionsBuilder<TStateBase>()
                .apply { fn() }
                .build()

            val newTransitionsByState = mutableMapOf<TStateBase, Transition<TStateBase>>()

            for (transition in newTransitions) {
                require(transition.next !in newTransitionsByState) {
                    "each parallel transition must point to a different state"
                }

                newTransitionsByState[transition.next] = transition
            }

            transitions[event] = newTransitionsByState
        }

        fun build() = Vertex<TStateBase, TEventBase>(state, transitions)
    }
}
