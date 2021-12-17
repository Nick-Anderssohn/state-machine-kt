package com.mrstatemachine.engine

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

        val stateMachine = StateMachine<State, Unit, Event> {
            startingState(State.LightOff)

            stateHandler(State.LightOff) {
                on<Event.OnClicked> {
                    execute { _, _ ->
                        transitionRecords += "light on"
                    }

                    transitionTo(State.LightOn)
                }
            }

            stateHandler(State.LightOn) {
                on<Event.OffClicked> {
                    execute { _, _ ->
                        transitionRecords += "light off"
                    }

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
