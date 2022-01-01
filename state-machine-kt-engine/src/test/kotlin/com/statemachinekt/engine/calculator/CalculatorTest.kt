package com.statemachinekt.engine.calculator

import com.statemachinekt.engine.StateMachine
import io.kotlintest.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.IllegalArgumentException

class CalculatorTest {
    companion object {
        private val SUPPORTED_OPERATORS = setOf('+', '-', '*', '/')
    }

    /**
     * Imagine an old-school calculator that has these buttons:
     *  "On/C" -> this button either turns the calculator on or clear it
     *  "Off"  -> turns the calculator off
     *  "+"    -> the plus sign
     *  "-"    -> the minus sign, or designates a negative number if it is the
     *            first button clicked when defining an operand.
     *  "*"    -> the multiplication sign
     *  "/"    -> the division sign
     *  "="    -> computes the result
     *  digits -> inputs the corresponding digit (e.x. the "1" button inputs a 1)
     *
     * If the calculator is currently empty, then numbers (or a negative sign) will be assigned to
     * the first operand. Once an operator is hit, then the display clears, and you start defining
     * the second operand. Then, if "=" is hit, the result is computed and set as the first operand.
     * You can continue to append digits to that result if you want since it is now just being used as
     * the first operand for your next equation. If "=" is not hit when defining the second operand
     * and a different operator is hit instead, then the result is computed, set as the first operand,
     * and you can start defining the second operand where the current operator is whatever you just hit.
     */
    private val calculator = StateMachine<State, ExtendedState, Event> {
        startingState(State.Off)
        startingExtendedState(ExtendedState())

        applyToAllStateDefinitions {
            on<Event.OnCClicked> {
                transitionTo(State.Operand1)

                // Clear whenever On/C is clicked
                execute { _ -> ExtendedState() }
            }

            on<Event.OffClicked> {
                transitionTo(State.Off)

                // Clear the state when off is clicked too
                // Yeah I know clicking on again would clear it anyways,
                // but I'm simulating a real calculator okay lol
                execute { _ -> ExtendedState() }
            }
        }

        stateDefinition(State.Off) { /* Doesn't do anything different than the defaults defined in applyToAllStates */ }

        stateDefinition(State.Operand1) {
            on<Event.NumClicked> {
                execute(Operands.First.handleNumber)
            }

            on<Event.PeriodClicked> {
                execute(Operands.First.handlePeriod)
            }

            on<Event.OperatorClicked> {
                // We don't have a transitionTo() call here because the nextState
                // is specified in Operands.First.handleOperator's return value.
                // This is done so that negative numbers can be handled.
                // If we wanted to though, we could have a transitionTo() defined
                // here that would act as the default if handleOperator returned
                // null for nextState.
                execute(Operands.First.handleOperator)
            }
        }

        stateDefinition(State.Operand2) {
            on<Event.NumClicked> {
                execute(Operands.Second.handleNumber)
            }

            on<Event.PeriodClicked> {
                execute(Operands.Second.handlePeriod)
            }

            on<Event.OperatorClicked> {
                // We don't have a transitionTo() call here because we want to just stay
                // in the Operand2 state in this case.
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

    @Test
    fun `calculator can handle negative numbers in the first operand`() = runBlocking {
        calculator.processEvents("-69 + 42 =")

        calculator.currentState shouldBe State.Operand1
        calculator.currentExtendedState shouldBe ExtendedState(operand1 = "-27")
    }

    @Test
    fun `calculator can handle negative numbers in the second operand`() = runBlocking {
        calculator.processEvents("69 * -42 =")

        calculator.currentState shouldBe State.Operand1
        calculator.currentExtendedState shouldBe ExtendedState(operand1 = "-2898")
    }

    @Test
    fun `calculator mega test`() = runBlocking {
        calculator.processEvents("0.69 * -42 + -82.5 * .1 / 7 =")

        calculator.currentState shouldBe State.Operand1
        calculator.currentExtendedState shouldBe ExtendedState(operand1 = "-1.5925714285714285")
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
