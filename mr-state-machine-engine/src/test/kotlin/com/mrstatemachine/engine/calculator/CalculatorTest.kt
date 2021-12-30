package com.mrstatemachine.engine.calculator

import com.mrstatemachine.engine.StateMachine
import io.kotlintest.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.IllegalArgumentException

// https://en.wikipedia.org/wiki/UML_state_machine#/media/File:UML_state_machine_Fig2a.png
class CalculatorTest {
    companion object {
        private val SUPPORTED_OPERATORS = setOf('+', '-', '*', '/')
    }

    private val calculator = StateMachine<State, ExtendedState, Event> {
        startingState(State.Off)

        startingExtendedState(ExtendedState())

        applyToAllStates {
            on<Event.OnCClicked> {
                transitionTo(State.Operand1)

                // Clear whenever On/C is clicked
                execute { _, _ -> ExtendedState() }
            }

            on<Event.OffClicked> {
                transitionTo(State.Off)

                // Clear the state when off is clicked too
                // Yeah I know clicking on again would clear it anyways
                // but I'm simulating a real calculator okay lol
                execute { _, _ -> ExtendedState() }
            }
        }

        stateHandler(State.Off) { /* Doesn't do anything different than the defaults defined in applyToAllStates */ }

        stateHandler(State.Operand1) {
            on<Event.NumClicked> {
                execute(Operands.First.handleNumber)
            }

            on<Event.PeriodClicked> {
                execute(Operands.First.handlePeriod)
            }

            on<Event.OperatorClicked> {
                // Doesn't support guards yet, so we can't handle negative numbers
                transitionTo(State.Operand2)
                execute(Operands.First.handleOperator)
            }
        }

        stateHandler(State.Operand2) {
            on<Event.NumClicked> {
                execute(Operands.Second.handleNumber)
            }

            on<Event.PeriodClicked> {
                execute(Operands.Second.handlePeriod)
            }

            on<Event.OperatorClicked> {
                transitionTo(State.Operand2)
                execute(Operands.Second.handleOperator)
            }

            on<Event.EqualsClicked> {
                transitionTo(State.Operand1)
                execute(Operands.Second.handleEquals)
            }
        }
    }

    @BeforeEach
    fun turnOnOrClearCalculator() = runBlocking { calculator.processEvent(Event.OnCClicked) }

    @Test
    fun `calculator integer addition test`() = runBlocking {
        calculator.processEvents("420 + 69 =")

        calculator.currentState shouldBe State.Operand1
        calculator.currentExtendedState shouldBe ExtendedState(operand1 = "489")
    }

    @Test
    fun `calculator double addition test 1`() = runBlocking {
        calculator.processEvents("4.20 + 69 =")

        calculator.currentState shouldBe State.Operand1
        calculator.currentExtendedState shouldBe ExtendedState(operand1 = "73.2")
    }

    @Test
    fun `calculator double addition test 2`() = runBlocking {
        calculator.processEvents("420 + 6.9 =")

        calculator.currentState shouldBe State.Operand1
        calculator.currentExtendedState shouldBe ExtendedState(operand1 = "426.9")
    }

    @Test
    fun `calculator double addition test 3`() = runBlocking {
        calculator.processEvents("420.420 + 6.9 =")

        calculator.currentState shouldBe State.Operand1
        calculator.currentExtendedState shouldBe ExtendedState(operand1 = "427.32")
    }

    @Test
    fun `calculator multiplication test`() = runBlocking {
        calculator.processEvents("420 * 69 =")

        calculator.currentState shouldBe State.Operand1
        calculator.currentExtendedState shouldBe ExtendedState(operand1 = "28980")
    }

    @Test
    fun `calculator division test`() = runBlocking {
        calculator.processEvents("420 / 69 =")

        calculator.currentState shouldBe State.Operand1
        calculator.currentExtendedState shouldBe ExtendedState(operand1 = "6.086956521739131")
    }

    @Test
    fun `calculator subtraction test`() = runBlocking {
        calculator.processEvents("420 - 69 =")

        calculator.currentState shouldBe State.Operand1
        calculator.currentExtendedState shouldBe ExtendedState(operand1 = "351")
    }

    @Test
    fun `calculator multiple operations test`() = runBlocking {
        // Keep in mind the string is a stream of events to our old-school
        // calculator state machine, so 420 - 69 happens first, not 69 * 2.
        // (The string thing is just to make testing easier, not intended
        // to be a part of the calculator)
        calculator.processEvents("420 - 69 * 2 =")

        calculator.currentState shouldBe State.Operand1
        calculator.currentExtendedState shouldBe ExtendedState(operand1 = "702")
    }

    private suspend fun StateMachine<State, ExtendedState, Event>.processEvents(input: String) {
        input.filter { !it.isWhitespace() }
            .map { it.toEvent() }
            .forEach { this.processEvent(it) }
    }

    private fun Char.toEvent() = when {
        this.isDigit() -> Event.NumClicked(this)
        this == '.' -> Event.PeriodClicked
        this == '=' -> Event.EqualsClicked
        this in SUPPORTED_OPERATORS -> Event.OperatorClicked(this)
        else -> throw IllegalArgumentException("unsupported character")
    }
}
