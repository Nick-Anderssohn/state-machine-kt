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
        val stateMachine = StateMachine<State, Event> {
            startingState(State.LightOff)

            state(State.LightOff) {
                on(Event.OnClicked) {
                    transitionTo(State.LightOn)
                }
            }

            state(State.LightOn) {
                on(Event.OffClicked) {
                    transitionTo(State.LightOff)
                }
            }
        }

        stateMachine.currentVertex.state shouldBe State.LightOff

        stateMachine.processEvent(Event.OnClicked)

        stateMachine.currentVertex.state shouldBe State.LightOn

        stateMachine.processEvent(Event.OffClicked)

        stateMachine.currentVertex.state shouldBe State.LightOff
    }
}
