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

@StateMachineDslMarker
class StateMachineBuilder<TStateBase : Any, TExtendedState : Any, TEventBase : Any> {
    private lateinit var acceptingState: TStateBase
    private var acceptingExtendedState: TExtendedState? = null
    private val vertexBuilders: MutableMap<TStateBase, VertexBuilder<TStateBase, TExtendedState, TEventBase, *, *>> = mutableMapOf()

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

    fun simpleStateHandler(
        state: TStateBase,
        fn: VertexBuilder<TStateBase, TExtendedState, TEventBase, Unit, Unit>.() -> Unit
    ): VertexBuilder<TStateBase, TExtendedState, TEventBase, Unit, Unit> = stateHandler<Unit, Unit>(state, fn)

    fun <TArrivalInput : Any, TArrivalOutput : Any> stateHandler(
        state: TStateBase,
        fn: VertexBuilder<TStateBase, TExtendedState, TEventBase, TArrivalInput, TArrivalOutput>.() -> Unit
    ): VertexBuilder<TStateBase, TExtendedState, TEventBase, TArrivalInput, TArrivalOutput> {
        require(state !in vertexBuilders) {
            "each state may only be defined once per state machine"
        }

        val vertexBuilder = VertexBuilder<TStateBase, TExtendedState, TEventBase, TArrivalInput, TArrivalOutput>(
            state = state,
            useOutputFromPreviousVertexAsInput = false
        )
            .apply(fn)

        vertexBuilders[state] = vertexBuilder

        return vertexBuilder
    }

    inline fun <
        TFirstArrivalOutput : Any,
        TSecondArrivalOutput : Any,
        reified TEvent : TEventBase
        > VertexBuilder<TStateBase, TExtendedState, TEventBase, *, TFirstArrivalOutput>.then(
        state: TStateBase
    ) {
        on<TEvent> {
            transitionTo(state)
        }
    }

    inline fun <
        TFirstArrivalOutput : Any,
        TSecondArrivalOutput : Any,
        reified TEvent : TEventBase
        > VertexBuilder<TStateBase, TExtendedState, TEventBase, *, TFirstArrivalOutput>.then(
        state: TStateBase,
        noinline fn: VertexBuilder<TStateBase, TExtendedState, TEventBase, TFirstArrivalOutput, TSecondArrivalOutput>.() -> Unit
    ): VertexBuilder<TStateBase, TExtendedState, TEventBase, TFirstArrivalOutput, TSecondArrivalOutput> {
        this.uponArrival {
            propagateEvent(TEvent::class.java)
        }

        this.on(TEvent::class.java) {
            transitionTo(state)
        }

        return this@StateMachineBuilder.stateHandler<TFirstArrivalOutput, TSecondArrivalOutput>(state, fn)
            .apply { useOutputFromPreviousVertexAsInput = true }
    }

    fun build() = StateMachine<TStateBase, TExtendedState, TEventBase>(
        stateStore = StateStore(
            currentState = acceptingState,
            extendedState = acceptingExtendedState
        ),
        vertices = vertexBuilders.map { it.key to it.value.build() }.toMap()
    )
}

@StateMachineDslMarker
class VertexBuilder<
    TStateBase : Any,
    TExtendedState : Any,
    TEventBase : Any,
    TArrivalInput : Any,
    TArrivalOutput : Any
    >internal constructor(
    private val state: TStateBase,

    @PublishedApi
    internal var useOutputFromPreviousVertexAsInput: Boolean
) {
    private val transitions = mutableMapOf<Class<out TEventBase>, StateTransition<TStateBase, *>>()

    private var arrivalBuilder: ArrivalBuilder<TStateBase, TExtendedState, TEventBase, TArrivalInput, TArrivalOutput>? = null

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
        arrivalBuilder = if (arrivalBuilder == null) {
            ArrivalBuilder<TStateBase, TExtendedState, TEventBase, TArrivalInput, TArrivalOutput>()
        } else {
            arrivalBuilder
        }

        arrivalBuilder!!.fn()
    }

    /**
     * Safe to call multiple times and make multiple copies of the vertex.
     * This is assuming that [state] is completely immutable.
     */
    fun build(): Vertex<TStateBase, TExtendedState, TEventBase, TArrivalInput, TArrivalOutput> {
        val arrivalBuildData = arrivalBuilder?.build()

        return Vertex<TStateBase, TExtendedState, TEventBase, TArrivalInput, TArrivalOutput>(
            state = state,
            transitions = transitions,
            stateProcessor = StateProcessor(
                onArrival = arrivalBuildData?.onArrival,
                eventsToPropagate = arrivalBuildData?.eventsToPropagate ?: emptySet(),
                merger = arrivalBuildData?.merger ?: NoOpMerger(),
                extractor = arrivalBuildData?.extractor,
                useOutputFromPreviousVertexAsInput = useOutputFromPreviousVertexAsInput
            )
        )
    }
}

@StateMachineDslMarker
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

    fun <TEvent : TEventBase> propagateEvent(clazz: Class<TEvent>) {
        this.result.eventsToPropagate.add(clazz)
    }

    fun storeExecutionOutput(merger: Merger<TOutput, TExtendedState>) {
        result.merger = merger
    }

    fun extractInputFromExtendedState(extractor: Extractor<TInput, TExtendedState>) {
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

@StateMachineDslMarker
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
