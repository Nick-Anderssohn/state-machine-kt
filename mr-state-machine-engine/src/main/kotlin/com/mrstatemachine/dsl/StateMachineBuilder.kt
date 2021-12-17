package com.mrstatemachine.dsl

import com.mrstatemachine.TransitionTask
import com.mrstatemachine.engine.Action
import com.mrstatemachine.engine.ExtendedStateStore
import com.mrstatemachine.engine.StateMachine
import com.mrstatemachine.engine.StateProcessor
import com.mrstatemachine.engine.StateStore
import com.mrstatemachine.engine.StateTransition
import com.mrstatemachine.engine.Vertex

@StateMachineDslMarker
class StateMachineBuilder<TStateBase : Any, TExtendedState : Any, TEventBase : Any> {
    private var acceptingStateInitialized = false
    private lateinit var acceptingExtendedState: TExtendedState

    private val extendedStateStore = ExtendedStateStore<TExtendedState>()

    @PublishedApi
    internal val stateStore = StateStore<TStateBase, TExtendedState>(
        extendedStateStore = extendedStateStore
    )

    @PublishedApi
    internal val vertexBuilders: MutableMap<TStateBase, VertexBuilder<TStateBase, TExtendedState, TEventBase>> = mutableMapOf()

    companion object {
        operator fun <TStateBase : Any, TExtendedState : Any, TEventBase : Any> invoke(
            fn: StateMachineBuilder<TStateBase, TExtendedState, TEventBase>.() -> Unit
        ) = StateMachineBuilder<TStateBase, TExtendedState, TEventBase>().apply(fn)
    }

    fun startingState(value: TStateBase) {
        require(!acceptingStateInitialized) {
            "state machine can only have one starting state"
        }

        acceptingStateInitialized = true
        this.stateStore.currentState = value
    }

    fun startingExtendedState(value: TExtendedState) {
        require(!this::acceptingExtendedState.isInitialized) {
            "state machine can only have one starting extended state"
        }

        this.acceptingExtendedState = value
    }

    inline fun stateHandler(
        state: TStateBase,
        fn: VertexBuilder<TStateBase, TExtendedState, TEventBase>.() -> Unit
    ): VertexBuilder<TStateBase, TExtendedState, TEventBase> {
        require(state !in vertexBuilders) {
            "each state may only be defined once per state machine"
        }

        val vertexBuilder = VertexBuilder<TStateBase, TExtendedState, TEventBase>(state, stateStore)
            .apply(fn)

        vertexBuilders[state] = vertexBuilder

        return vertexBuilder
    }

    inline fun <reified TEvent : TEventBase> VertexBuilder<TStateBase, TExtendedState, TEventBase>.then(
        state: TStateBase
    ) {
        on<TEvent> {
            transitionTo(state)
        }
    }

    inline fun <reified TEvent : TEventBase> VertexBuilder<TStateBase, TExtendedState, TEventBase>.then(
        state: TStateBase,
        noinline fn: VertexBuilder<TStateBase, TExtendedState, TEventBase>.() -> Unit
    ): VertexBuilder<TStateBase, TExtendedState, TEventBase> {
        this.uponArrival {
            propagateEvent(TEvent::class.java)
        }

        this.on(TEvent::class.java) {
            transitionTo(state)
        }

        return this@StateMachineBuilder.stateHandler(state, fn)
    }

    fun build() = StateMachine<TStateBase, TExtendedState, TEventBase>(
        stateStore = stateStore,
        vertices = vertexBuilders.map { it.key to it.value.build() }.toMap()
    )
}

@StateMachineDslMarker
class VertexBuilder<
    TStateBase : Any,
    TExtendedState : Any,
    TEventBase : Any
    >@PublishedApi internal constructor(
    private val state: TStateBase,
    private val stateStore: StateStore<TStateBase, TExtendedState>
) {
    private val transitions = mutableMapOf<Class<out TEventBase>, StateTransition<TStateBase, *>>()

    private var arrivalBuilder: ArrivalBuilder<TStateBase, TExtendedState, TEventBase>? = null

    inline fun <reified TEvent : TEventBase> on(noinline fn: TransitionBuilder<TStateBase, TEvent>.() -> Unit) {
        on(TEvent::class.java, fn)
    }

    @PublishedApi
    internal fun <TEvent : TEventBase> on(
        eventClass: Class<TEvent>,
        fn: TransitionBuilder<TStateBase, TEvent>.() -> Unit
    ) {
        require(eventClass !in transitions) { "you may only register each event once per state" }

        transitions[eventClass] = TransitionBuilder<TStateBase, TEvent>(state)
            .apply(fn)
            .build()
    }

    fun uponArrival(fn: ArrivalBuilder<TStateBase, TExtendedState, TEventBase>.() -> Unit) {
        arrivalBuilder = if (arrivalBuilder == null) {
            ArrivalBuilder<TStateBase, TExtendedState, TEventBase>()
        } else {
            arrivalBuilder
        }

        arrivalBuilder!!.fn()
    }

    /**
     * Safe to call multiple times and make multiple copies of the vertex.
     * This is assuming that [state] is completely immutable.
     */
    fun build(): Vertex<TStateBase, TExtendedState, TEventBase> {
        val arrivalBuildData = arrivalBuilder?.build()

        return Vertex<TStateBase, TExtendedState, TEventBase>(
            state = state,
            transitions = transitions,
            stateProcessor = StateProcessor(
                stateStore = stateStore,
                onArrival = arrivalBuildData?.onArrival,
                eventsToPropagate = arrivalBuildData?.eventsToPropagate ?: emptySet()
            )
        )
    }
}

@StateMachineDslMarker
class ArrivalBuilder<TStateBase : Any, TExtendedState : Any, TEventBase : Any>internal constructor() {
    private val result = BuildData<TStateBase, TExtendedState, TEventBase>()

    fun execute(fn: Action<TEventBase, TExtendedState>) {
        this.result.onArrival = fn
    }

    internal inline fun <reified TEvent : TEventBase> propagateEvent() {
        propagateEvent(TEvent::class.java)
    }

    fun <TEvent : TEventBase> propagateEvent(clazz: Class<TEvent>) {
        this.result.eventsToPropagate.add(clazz)
    }

    fun build() = result

    data class BuildData<TStateBase : Any, TExtendedState : Any, TEventBase : Any>(
        var eventsToPropagate: MutableSet<Class<out TEventBase>> = mutableSetOf(),
        var onArrival: Action<TEventBase, TExtendedState>? = null
    )
}

@StateMachineDslMarker
class TransitionBuilder<TStateBase : Any, TEvent : Any>internal constructor(
    private val currentState: TStateBase
) {
    private var nextState: TStateBase? = null

    private var task: TransitionTask<TEvent>? = null

    fun transitionTo(nextState: TStateBase) {
        require(this.nextState == null) {
            "next state is already configured to $nextState"
        }

        this.nextState = nextState
    }

    fun execute(task: TransitionTask<TEvent>?) {
        require(this.task == null) {
            "there can only be one task per transition"
        }

        this.task = task
    }

    internal fun build(): StateTransition<TStateBase, TEvent> = StateTransition(
        next = checkNotNull(nextState),
        task = task
    )
}
