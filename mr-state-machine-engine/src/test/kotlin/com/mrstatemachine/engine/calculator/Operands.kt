package com.mrstatemachine.engine.calculator

import com.mrstatemachine.engine.TransitionTask
import java.lang.IllegalStateException

sealed class Operands(
    private val getOperand: ExtendedState.() -> String,
    private val updateOperand: ExtendedState.(newOperand: String) -> ExtendedState
) {
    val handleNumber = TransitionTask<Event.NumClicked, ExtendedState> { event, stateStore ->
        stateStore.extendedState.updateOperand(
            stateStore.extendedState.getOperand() + event.digit
        )
    }

    val handlePeriod = TransitionTask<Event.PeriodClicked, ExtendedState> { _, stateStore ->
        stateStore.extendedState.let {
            if (it.getOperand().contains('.')) {
                it
            } else {
                stateStore.extendedState.updateOperand(it.getOperand() + '.')
            }
        }
    }

    object First : Operands(
        getOperand = { this.operand1 },
        updateOperand = { copy(operand1 = it) }
    ) {
        val handleOperator = TransitionTask<Event.OperatorClicked, ExtendedState> { event, stateStore ->
            stateStore.extendedState.copy(operator = event.operator)
        }
    }

    object Second : Operands(
        getOperand = { this.operand2 },
        updateOperand = { copy(operand2 = it) }
    ) {
        val handleOperator = TransitionTask<Event.OperatorClicked, ExtendedState> { event, stateStore ->
            stateStore.extendedState.compute().copy(operator = event.operator)
        }

        val handleEquals = TransitionTask<Event.EqualsClicked, ExtendedState> { _, stateStore ->
            stateStore.extendedState.compute()
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
                .toString()
        )
    }
}
