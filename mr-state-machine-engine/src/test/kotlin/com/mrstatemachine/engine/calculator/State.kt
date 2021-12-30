package com.mrstatemachine.engine.calculator

sealed class State {
    object Off : State()
    object Operand1 : State()
    object Operand2 : State()
}

data class ExtendedState(
    val operand1: String = "",
    val operator: Char? = null,
    val operand2: String = "",
)

sealed class Event {
    object OnCClicked : Event()
    object OffClicked : Event()
    object EqualsClicked : Event()
    object PeriodClicked : Event()

    data class NumClicked(val digit: Char) : Event()
    data class OperatorClicked(val operator: Char) : Event()
}
