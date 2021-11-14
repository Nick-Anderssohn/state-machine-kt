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
        val transitionRecords: MutableList<String> = mutableListOf()

        val turnLightOn = object : Task {
            override suspend fun run() {
                transitionRecords += "light on"
                return Unit
            }
        }

        val turnLightOff = object : Task {
            override suspend fun run() {
                transitionRecords += "light off"
                return Unit
            }
        }

        val stateMachine = StateMachine<State, Event> {
            startingState(State.LightOff)

            state(State.LightOff) {
                on(Event.OnClicked) {
                    execute(turnLightOn)
                    transitionTo(State.LightOn)
                }
            }

            state(State.LightOn) {
                on(Event.OffClicked) {
                    execute(turnLightOff)
                    transitionTo(State.LightOff)
                }
            }
        }

        runBlocking {
            stateMachine.currentVertices.first().state shouldBe State.LightOff

            stateMachine.processEvent(Event.OnClicked)

            transitionRecords shouldBe mutableListOf("light on")

            stateMachine.currentVertices.first().state shouldBe State.LightOn

            stateMachine.processEvent(Event.OffClicked)

            transitionRecords shouldBe mutableListOf(
                "light on",
                "light off"
            )

            stateMachine.currentVertices.first().state shouldBe State.LightOff
        }
    }
}
