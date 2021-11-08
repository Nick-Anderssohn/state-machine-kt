package com.mrstatemachine.engine

import com.mrstatemachine.Task
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
        val records: MutableList<String> = mutableListOf()

        val turnLightOn = object : Task {
            override suspend fun run(): Any {
                records += "light on"
                return Unit
            }
        }

        val turnLightOff = object : Task {
            override suspend fun run(): Any {
                records += "light off"
                return Unit
            }
        }

        val stateMachine = StateMachine<State, Event> {
            startingState(State.LightOff)

            state(State.LightOff) {
                on(Event.OnClicked) {
                    transitionTo(State.LightOn)
                    transitionTask(turnLightOn)
                }
            }

            state(State.LightOn) {
                on(Event.OffClicked) {
                    transitionTo(State.LightOff)
                    transitionTask(turnLightOff)
                }
            }
        }

        runBlocking {
            stateMachine.currentVertex.state shouldBe State.LightOff

            stateMachine.processEvent(Event.OnClicked)

            records shouldBe mutableListOf("light on")

            stateMachine.currentVertex.state shouldBe State.LightOn

            stateMachine.processEvent(Event.OffClicked)

            records shouldBe mutableListOf(
                "light on",
                "light off"
            )

            stateMachine.currentVertex.state shouldBe State.LightOff
        }
    }
}
