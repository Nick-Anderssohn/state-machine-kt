package com.mrstatemachine.engine

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

        val togglePower = TransitionTask<Event.PowerToggled, Unit> { event: Event.PowerToggled, _ ->
            transitionRecords += when (event.newPosition) {
                Position.ON -> "light on"
                Position.OFF -> "light off"
            }
        }

        val stateMachine = StateMachine<State, Unit, Event> {
            startingState(State.LightOff)

            stateDefinition(State.LightOff) {
                on<Event.PowerToggled> {
                    execute(togglePower)
                    transitionTo(State.LightOn)
                }
            }

            stateDefinition(State.LightOn) {
                on<Event.PowerToggled> {
                    execute(togglePower)
                    transitionTo(State.LightOff)
                }
            }
        }

        runBlocking {
            stateMachine.currentVertex.state shouldBe State.LightOff

            stateMachine.processEvent(Event.PowerToggled(Position.ON))

            transitionRecords shouldBe mutableListOf("light on")

            stateMachine.currentVertex.state shouldBe State.LightOn

            stateMachine.processEvent(Event.PowerToggled(Position.OFF))

            transitionRecords shouldBe mutableListOf(
                "light on",
                "light off"
            )

            stateMachine.currentVertex.state shouldBe State.LightOff
        }
    }
}
