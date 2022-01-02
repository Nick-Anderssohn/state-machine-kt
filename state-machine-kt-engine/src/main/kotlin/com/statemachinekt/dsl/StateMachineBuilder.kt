package com.statemachinekt.dsl

import com.statemachinekt.engine.Action
import com.statemachinekt.engine.ExtendedStateStore
import com.statemachinekt.engine.StateMachine
import com.statemachinekt.engine.StateMachineConfig
import com.statemachinekt.engine.StateStore
import com.statemachinekt.engine.StateTransition
import com.statemachinekt.engine.TransitionTask
import com.statemachinekt.engine.TransitionTaskResult
import com.statemachinekt.engine.Vertex

@StateMachineDslMarker
class StateMachineBuilder<TStateBase : Any, TExtendedState : Any, TEventBase : Any> {
    private lateinit var acceptingState: TStateBase
    private lateinit var acceptingExtendedState: TExtendedState

    private var stateMachineConfig: StateMachineConfig? = null

    private val extendedStateStore by lazy {
        check(this::acceptingExtendedState.isInitialized) {
            "you must call startingExtendedState() towards the top of your state machine definition"
        }

        ExtendedStateStore<TExtendedState>(acceptingExtendedState)
    }

    private var superVertexBuilder: VertexBuilder<TStateBase, TExtendedState, TEventBase>? = null

    @PublishedApi
    internal val stateStore by lazy {
        check(this::acceptingState.isInitialized) {
            "you must call startingState() towards the top of your state machine definition"
        }

        StateStore(
            acceptingState = acceptingState,
            extendedStateStore = extendedStateStore
        )
    }

    @PublishedApi
    internal val vertexBuilders: MutableMap<TStateBase, VertexBuilder<TStateBase, TExtendedState, TEventBase>> = mutableMapOf()

    companion object {
        operator fun <TStateBase : Any, TExtendedState : Any, TEventBase : Any> invoke(
            fn: StateMachineBuilder<TStateBase, TExtendedState, TEventBase>.() -> Unit
        ) = StateMachineBuilder<TStateBase, TExtendedState, TEventBase>().apply(fn)
    }

    fun startingState(value: TStateBase) {
        require(!this::acceptingState.isInitialized) {
            "state machine can only have one starting state"
        }

        acceptingState = value
    }

    fun withConfig(config: StateMachineConfig) {
        this.stateMachineConfig = config
    }

    fun startingExtendedState(value: TExtendedState) {
        require(!this::acceptingExtendedState.isInitialized) {
            "state machine can only have one starting extended state"
        }

        this.acceptingExtendedState = value
    }

    fun applyToAllStateDefinitions(
        fn: VertexBuilder<TStateBase, TExtendedState, TEventBase>.() -> Unit
    ) {
        check(superVertexBuilder == null) {
            "can only call applyToAllStates once"
        }

        superVertexBuilder = VertexBuilder<TStateBase, TExtendedState, TEventBase>(null, stateStore)
            .apply(fn)
    }

    fun stateDefinition(
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

    fun build() = StateMachine(
        stateStore = stateStore,
        vertices = vertexBuilders.map { it.key to it.value.build() }.toMap(),
        superVertex = superVertexBuilder?.build()
            ?: VertexBuilder<TStateBase, TExtendedState, TEventBase>(null, stateStore).build(),
        config = stateMachineConfig ?: StateMachineConfig()
    )
}

@StateMachineDslMarker
class VertexBuilder<
    TStateBase : Any,
    TExtendedState : Any,
    TEventBase : Any
    >@PublishedApi internal constructor(
    private val state: TStateBase?,
    private val stateStore: StateStore<TStateBase, TExtendedState>
) {
    private val transitions = mutableMapOf<Class<out TEventBase>, StateTransition<TStateBase, TExtendedState, *>>()

    private var arrivalBuilder: ActionBuilder<TStateBase, TExtendedState, TEventBase>? = null

    private var exitBuilder: ActionBuilder<TStateBase, TExtendedState, TEventBase>? = null

    inline fun <reified TEvent : TEventBase> on(noinline fn: TransitionBuilder<TStateBase, TExtendedState, TEvent>.() -> Unit) {
        on(TEvent::class.java, fn)
    }

    @PublishedApi
    internal fun <TEvent : TEventBase> on(
        eventClass: Class<TEvent>,
        fn: TransitionBuilder<TStateBase, TExtendedState, TEvent>.() -> Unit
    ) {
        require(eventClass !in transitions) { "you may only register each event once per state" }

        transitions[eventClass] = TransitionBuilder<TStateBase, TExtendedState, TEvent>()
            .apply(fn)
            .build()
    }

    fun uponArrival(fn: ActionBuilder<TStateBase, TExtendedState, TEventBase>.() -> Unit) {
        arrivalBuilder = if (arrivalBuilder == null) {
            ActionBuilder()
        } else {
            arrivalBuilder
        }

        arrivalBuilder!!.fn()
    }

    fun uponExit(fn: ActionBuilder<TStateBase, TExtendedState, TEventBase>.() -> Unit) {
        exitBuilder = if (exitBuilder == null) {
            ActionBuilder()
        } else {
            exitBuilder
        }

        exitBuilder!!.fn()
    }

    internal fun build() = Vertex(
        state = state,
        transitions = transitions,
        stateStore = stateStore,
        onArrival = arrivalBuilder?.build()?.action,
        onExit = exitBuilder?.build()?.action
    )
}

@StateMachineDslMarker
class ActionBuilder<TStateBase : Any, TExtendedState : Any, TEventBase : Any>internal constructor() {
    private val result = BuildData<TExtendedState, TEventBase>()

    fun execute(fn: Action<TEventBase, TExtendedState>) {
        this.result.action = fn
    }

    internal fun build() = result

    data class BuildData<TExtendedState : Any, TEventBase : Any>(
        var action: Action<TEventBase, TExtendedState>? = null
    )
}

@StateMachineDslMarker
class TransitionBuilder<TStateBase : Any, TExtendedState : Any, TEvent : Any>internal constructor() {
    private var nextState: TStateBase? = null

    private var task: TransitionTask<TEvent, TStateBase, TExtendedState>? = null

    fun transitionTo(nextState: TStateBase) {
        require(this.nextState == null) {
            "next state is already configured to $nextState"
        }

        this.nextState = nextState
    }

    fun execute(task: TransitionTask<TEvent, TStateBase, TExtendedState>?) {
        require(this.task == null) {
            "there can only be one task per transition"
        }

        this.task = task
    }

    fun execute(fn: (extendedStateStore: ExtendedStateStore<TExtendedState>) -> TExtendedState) {
        execute { _, store ->
            TransitionTaskResult(fn(store))
        }
    }

    fun execute(fn: () -> Unit) = execute { _, store ->
        fn()
        TransitionTaskResult(store.extendedState)
    }

    internal fun build(): StateTransition<TStateBase, TExtendedState, TEvent> = StateTransition(
        next = nextState,
        task = task
    )
}
