package com.mrstatemachine.engine

import com.mrstatemachine.dsl.StateMachineBuilder
import org.junit.jupiter.api.Test

// https://en.wikipedia.org/wiki/UML_state_machine#/media/File:UML_state_machine_Fig2a.png
class CalculatorTest {
    sealed class Event {
        object OnCClicked : Event()
        object OffClicked : Event()
        object EqualsClicked : Event()
        object PeriodClicked : Event()

        data class NumClicked(val digit: Int) : Event()
        data class OperatorClicked(val operator: Char) : Event()
    }

    sealed class State {
        object Off : State()
        object Operand1 : State()
        object OpEntered : State()
        object Operand2 : State()
    }

    data class ExtendedState(
        val operand1: Double? = null,
        val operator: Char? = null,
        val operand2: Double? = null
    )

    val calculatorBuilder = StateMachineBuilder<State, Unit, Event> {
        startingState(State.Off)

        stateHandler(State.Off) {
            on<Event.OnCClicked> {
                transitionTo(State.Operand1)
            }
        }

        stateHandler(State.Operand1) {
            on<Event.OnCClicked> {
            }
        }
    }

    @Test
    fun `calculator traditional FSM test`() {
    }
}
