package com.mrstatemachine.engine.calculator

import com.mrstatemachine.engine.TransitionTask
import com.mrstatemachine.engine.TransitionTaskResult
import java.lang.IllegalStateException

sealed class Operands(
    private val getOperand: ExtendedState.() -> String,
    private val updateOperand: ExtendedState.(newOperand: String) -> ExtendedState
) {
    val handleNumber = TransitionTask<Event.NumClicked, State, ExtendedState> { event, stateStore ->
        TransitionTaskResult(
            stateStore.extendedState.updateOperand(
                stateStore.extendedState.getOperand() + event.digit
            )
        )
    }

    val handlePeriod = TransitionTask<Event.PeriodClicked, State, ExtendedState> { _, stateStore ->
        TransitionTaskResult(
            stateStore.extendedState.let {
                if (it.getOperand().contains('.')) {
                    it
                } else {
                    stateStore.extendedState.updateOperand(it.getOperand() + '.')
                }
            }
        )
    }

    object First : Operands(
        getOperand = { this.operand1 },
        updateOperand = { copy(operand1 = it) }
    ) {
        val handleOperator = TransitionTask<Event.OperatorClicked, State, ExtendedState> { event, stateStore ->
            // If our first operand is currently empty, then the user wants a negative number.
            // Otherwise, they want the operator to be the minus sign and move onto the 2nd operand.
            if (event.operator == '-' && stateStore.extendedState.operand1.isEmpty()) {
                TransitionTaskResult(
                    extendedState = stateStore.extendedState.copy(operand1 = "-"),
                    nextState = State.Operand1
                )
            } else {
                TransitionTaskResult(
                    extendedState = stateStore.extendedState.copy(operator = event.operator),
                    nextState = State.Operand2
                )
            }
        }
    }

    object Second : Operands(
        getOperand = { this.operand2 },
        updateOperand = { copy(operand2 = it) }
    ) {
        val handleOperator = TransitionTask<Event.OperatorClicked, State, ExtendedState> { event, stateStore ->
            if (event.operator == '-' && stateStore.extendedState.operand2.isEmpty()) {
                TransitionTaskResult(
                    extendedState = stateStore.extendedState.copy(operand2 = "-"),
                    // since we aren't specifying a nextState, and the state machine doesn't
                    // have transitionTo() defined for this state/event combo, the state machine
                    // will remain in the Operand2 state.
                )
            } else {
                TransitionTaskResult(
                    extendedState = stateStore.extendedState.compute().copy(operator = event.operator),

                    // Doesn't hurt to specify the state anyways though
                    nextState = State.Operand2
                )
            }
        }

        val handleEquals = TransitionTask<Event.EqualsClicked, State, ExtendedState> { _, stateStore ->
            TransitionTaskResult(stateStore.extendedState.compute())
        }

        private fun ExtendedState.compute() = ExtendedState(
            operand1 = when (operator) {
                '+' -> operand1.toDouble() + operand2.toDouble()
                '-' -> operand1.toDouble() - operand2.toDouble()
                '*' -> operand1.toDouble() * operand2.toDouble()
                '/' -> operand1.toDouble() / operand2.toDouble()
                else -> throw IllegalStateException("unsupported operator")
            }
                .let {
                    if ((it % 1.0) == 0.0) {
                        it.toInt()
                    } else {
                        it
                    }
                }
                .toString(),
            operator = null,
            operand2 = ""
        )
    }
}
