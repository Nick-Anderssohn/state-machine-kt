package com.mrstatemachine.engine

import com.mrstatemachine.TransitionTask
import io.kotlintest.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class AlternativeLightBulbTest {
    enum class Position {
        ON,
        OFF
    }

    sealed class Event {
        data class PowerToggled(
            val newPosition: Position
        ) : Event()
    }

    sealed class State {
        object LightOn : State()
        object LightOff : State()
    }

    @Test
    fun `alternative light bulb state machine transitions correctly`() {
        val transitionRecords: MutableList<String> = mutableListOf()

        val togglePower = TransitionTask<Event.PowerToggled> { event: Event.PowerToggled ->
            transitionRecords += when (event.newPosition) {
                Position.ON -> "light on"
                Position.OFF -> "light off"
            }
        }

        val stateMachine = StateMachine<State, Event> {
            startingState(State.LightOff)

            state(State.LightOff) {
                on<Event.PowerToggled> {
                    execute(togglePower)
                    transitionTo(State.LightOn)
                }
            }

            state(State.LightOn) {
                on<Event.PowerToggled> {
                    execute(togglePower)
                    transitionTo(State.LightOff)
                }
            }
        }

        runBlocking {
            stateMachine.currentVertices.first().state shouldBe State.LightOff

            stateMachine.processEvent(Event.PowerToggled(Position.ON))

            transitionRecords shouldBe mutableListOf("light on")

            stateMachine.currentVertices.first().state shouldBe State.LightOn

            stateMachine.processEvent(Event.PowerToggled(Position.OFF))

            transitionRecords shouldBe mutableListOf(
                "light on",
                "light off"
            )

            stateMachine.currentVertices.first().state shouldBe State.LightOff
        }
    }
}
