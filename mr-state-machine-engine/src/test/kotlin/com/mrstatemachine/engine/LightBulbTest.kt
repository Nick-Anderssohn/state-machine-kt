package com.mrstatemachine.engine

import com.mrstatemachine.TransitionTask
import io.kotlintest.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class LightBulbTest {
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
        val transitionRecords: MutableList<String> = mutableListOf()

        val turnLightOn = TransitionTask<Event.OnClicked> {
            transitionRecords += "light on"
        }

        val turnLightOff = TransitionTask<Event.OffClicked> {
            transitionRecords += "light off"
        }

        val stateMachine = StateMachine<State, Unit, Event> {
            startingState(State.LightOff)

            state<Unit, Unit>(State.LightOff) {
                on<Event.OnClicked> {
                    execute(turnLightOn)
                    transitionTo(State.LightOn)
                }
            }

            state<Unit, Unit>(State.LightOn) {
                on<Event.OffClicked> {
                    execute(turnLightOff)
                    transitionTo(State.LightOff)
                }
            }
        }

        runBlocking {
            stateMachine.currentVertex.state shouldBe State.LightOff

            stateMachine.processEvent(Event.OnClicked)

            transitionRecords shouldBe mutableListOf("light on")

            stateMachine.currentVertex.state shouldBe State.LightOn

            stateMachine.processEvent(Event.OffClicked)

            transitionRecords shouldBe mutableListOf(
                "light on",
                "light off"
            )

            stateMachine.currentVertex.state shouldBe State.LightOff
        }
    }
}
