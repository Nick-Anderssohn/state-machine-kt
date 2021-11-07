package com.mrstatemachine.engine

import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test

class StateMachineTest {
    sealed class Event {
        object OnClicked : Event()
        object OffClicked : Event()
    }

    sealed class State {
        object LightOn : State()
        object LightOff : State()
    }

    @Test
    fun `light bulb state machine transitions correctly`() {
        val onState = Vertex<Event, State>(
            state = State.LightOn
        )

        val offState = Vertex<Event, State>(
            state = State.LightOff,
            transitions = mutableMapOf(
                Event.OnClicked to Transition(onState)
            )
        )

        onState.transitions[Event.OffClicked] = Transition(offState)

        val stateMachine = StateMachine<Event, State>(
            acceptingState = offState
        )

        stateMachine.currentVertex shouldBe onState

        stateMachine.processEvent(Event.OnClicked)

        stateMachine.currentVertex shouldBe onState

        stateMachine.processEvent(Event.OffClicked)

        stateMachine.currentVertex shouldBe offState
    }
}
