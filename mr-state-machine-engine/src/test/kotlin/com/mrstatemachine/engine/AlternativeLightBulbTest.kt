package com.mrstatemachine.engine

import org.junit.jupiter.api.Test

class AlternativeLightBulbTest {
    enum class Position {
        ON,
        OFF
    }

    sealed class Event {
        data class PowerToggled(
            val newPosition: Position
        )
    }

    sealed class State {
        object LightOn : State()
        object LightOff : State()
    }

    @Test
    fun `alternative light bulb state machine transitions correctly`() {
//        val stateMachine = StateMachine<State, Event> {
//            startingState(State.LightOff)
//
//            state(State.LightOff) {
//                on(Event.PowerToggled)
//            }
//        }
    }
}
