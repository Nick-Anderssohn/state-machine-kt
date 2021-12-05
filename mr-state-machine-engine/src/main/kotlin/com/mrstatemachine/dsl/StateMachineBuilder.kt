package com.mrstatemachine.dsl

import com.mrstatemachine.Extractor
import com.mrstatemachine.Merger
import com.mrstatemachine.NoOpMerger
import com.mrstatemachine.TransitionTask
import com.mrstatemachine.engine.StateMachine
import com.mrstatemachine.engine.StateProcessor
import com.mrstatemachine.engine.StateStore
import com.mrstatemachine.engine.StateTransition
import com.mrstatemachine.engine.Vertex

class StateMachineBuilder<TStateBase : Any, TExtendedState : Any, TEventBase : Any> {
    private lateinit var acceptingState: TStateBase
    private var acceptingExtendedState: TExtendedState? = null
    private val vertices: MutableMap<TStateBase, Vertex<TStateBase, TExtendedState, TEventBase, *, *>> = mutableMapOf()

    companion object {
        operator fun <TStateBase : Any, TExtendedState : Any, TEventBase : Any> invoke(
            fn: StateMachineBuilder<TStateBase, TExtendedState, TEventBase>.() -> Unit
        ) = StateMachineBuilder<TStateBase, TExtendedState, TEventBase>().apply(fn)
    }

    fun startingState(value: TStateBase) {
        require(!this::acceptingState.isInitialized) {
            "state machine can only have one starting state"
        }

        this.acceptingState = value
    }

    // TODO: Switch to builder or lambda for extended state so that it is safe
    //  to call StateMachineBuilder.build() multiple times.
    fun startingExtendedState(value: TExtendedState) {
        require(acceptingExtendedState == null) {
            "state machine can only have one starting extended state"
        }

        this.acceptingExtendedState = value
    }

    fun <TArrivalInput : Any, TArrivalOutput : Any> state(
        state: TStateBase,
        fn: VertexBuilder<TStateBase, TExtendedState, TEventBase, TArrivalInput, TArrivalOutput>.() -> Unit
    ) {
        require(state !in vertices) {
            "each state may only be defined once per state machine"
        }

        vertices[state] = VertexBuilder<TStateBase, TExtendedState, TEventBase, TArrivalInput, TArrivalOutput>(state)
            .apply(fn)
            .build()
    }

    fun build() = StateMachine<TStateBase, TExtendedState, TEventBase>(
        StateStore(
            currentState = acceptingState,
            extendedState = acceptingExtendedState
        ),
        vertices
    )
}

class VertexBuilder<
    TStateBase : Any,
    TExtendedState : Any,
    TEventBase : Any,
    TArrivalInput : Any,
    TArrivalOutput : Any
    >internal constructor(
    private val state: TStateBase
) {
    private val transitions = mutableMapOf<Class<out TEventBase>, StateTransition<TStateBase, *>>()

    private var arrivalBuildData: ArrivalBuilder.BuildData<TStateBase, TExtendedState, TEventBase, TArrivalInput, TArrivalOutput>? = null

    inline fun <reified TEvent : TEventBase> on(noinline fn: TransitionBuilder<TStateBase, TEvent>.() -> Unit) {
        on(TEvent::class.java, fn)
    }

    @PublishedApi
    internal fun <TEvent : TEventBase> on(
        eventClass: Class<TEvent>,
        fn: TransitionBuilder<TStateBase, TEvent>.() -> Unit
    ) {
        require(eventClass !in transitions) { "you may only register each event once per state" }

        transitions[eventClass] = TransitionBuilder<TStateBase, TEvent>()
            .apply(fn)
            .build()
    }

    fun uponArrival(fn: ArrivalBuilder<TStateBase, TExtendedState, TEventBase, TArrivalInput, TArrivalOutput>.() -> Unit) {
        val builder = ArrivalBuilder<TStateBase, TExtendedState, TEventBase, TArrivalInput, TArrivalOutput>()
        builder.fn()
        arrivalBuildData = builder.build()
    }

    /**
     * Safe to call multiple times and make multiple copies of the vertex.
     * This is assuming that [state] is completely immutable.
     */
    fun build() = Vertex<TStateBase, TExtendedState, TEventBase, TArrivalInput, TArrivalOutput>(
        state = state,
        transitions = transitions,
        stateProcessor = StateProcessor(
            onArrival = arrivalBuildData?.onArrival,
            eventsToPropagate = arrivalBuildData?.eventsToPropagate ?: emptySet(),
            merger = arrivalBuildData?.merger ?: NoOpMerger(),
            extractor = arrivalBuildData?.extractor
        )
    )
}

class ArrivalBuilder<
    TStateBase : Any,
    TExtendedState : Any,
    TEventBase : Any,
    TInput : Any,
    TOutput : Any
    >internal constructor() {
    private val result = BuildData<TStateBase, TExtendedState, TEventBase, TInput, TOutput>()

    fun execute(fn: suspend (input: TInput) -> TOutput) {
        this.result.onArrival = fn
    }

    internal inline fun <reified TEvent : TEventBase> propagateEvent() {
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

class TransitionBuilder<TStateBase : Any, TEvent : Any>internal constructor() {
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
