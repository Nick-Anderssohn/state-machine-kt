package com.mrstatemachine.engine

import com.google.gson.Gson
import io.kotlintest.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

data class ExtractedData(
    val raw: String
)

data class TransformedData(
    val foo: String,
    val bar: String
)

class EtlTest {
    sealed class Event {
        object Run : Event()
    }

    sealed class State {
        object Waiting : State()
        object Extracting : State()
        object Transforming : State()
        object Loading : State()
    }

    data class ExtendedState(
        val extractedData: ExtractedData? = null,
        val transformedData: TransformedData? = null
    )

    private val gson = Gson()

    @Test
    fun `etl state machine runs completely when defined using then`() {
        var loadedData: TransformedData? = null

        val etl = StateMachine<State, Unit, Event> {
            startingState(State.Waiting)

            simpleStateHandler(State.Waiting) {
                on<Event.Run> {
                    transitionTo(State.Extracting)
                }
            }

            stateHandler<Unit, ExtractedData>(State.Extracting) {
                uponArrival {
                    extractInputFromExtendedState {}

                    execute {
                        println("retrieving data...")
                        ExtractedData(raw = "{\"foo\": \"I'm foo!\",\"bar\": \"I'm bar!\"}")
                    }
                }
            }
                .then<ExtractedData, TransformedData, Event.Run>(State.Transforming) {
                    uponArrival {
                        execute { extractedData ->
                            println("transforming data...")
                            gson.fromJson(extractedData.raw)
                        }
                    }
                }
                .then<TransformedData, Unit, Event.Run>(State.Loading) {
                    uponArrival {
                        execute { transformedData ->
                            println("uploading data...")
                            loadedData = transformedData
                        }
                    }
                }
                .then<Unit, Unit, Event.Run>(State.Waiting)
        }

        runBlocking {
            etl.processEvent(Event.Run)
        }

        loadedData shouldBe TransformedData(
            foo = "I'm foo!",
            bar = "I'm bar!"
        )

        with(etl.stateStore) {
            currentState shouldBe State.Waiting
            extendedState shouldBe null
        }
    }

    @Test
    fun `etl state machine runs completely when defined using propagateEvent`() {
        var loadedData: TransformedData? = null

        val etl = StateMachine<State, ExtendedState, Event> {
            startingState(State.Waiting)

            stateHandler<Unit, Unit>(State.Waiting) {
                on<Event.Run> {
                    transitionTo(State.Extracting)
                }
            }

            stateHandler<Unit, ExtractedData>(State.Extracting) {
                uponArrival {
                    extractInputFromExtendedState {}

                    execute {
                        println("retrieving data...")
                        ExtractedData(raw = "{\"foo\": \"I'm foo!\",\"bar\": \"I'm bar!\"}")
                    }

                    storeExecutionOutput { extractedData, extendedState ->
                        (extendedState ?: ExtendedState()).copy(extractedData = extractedData)
                    }

                    propagateEvent<Event.Run>()
                }

                on<Event.Run> {
                    transitionTo(State.Transforming)
                }
            }

            stateHandler<ExtractedData, TransformedData>(State.Transforming) {
                uponArrival {
                    extractInputFromExtendedState { it!!.extractedData!! }

                    execute { extractedData ->
                        println("transforming data...")
                        gson.fromJson(extractedData.raw)
                    }

                    storeExecutionOutput { transformedData, extendedState ->
                        (extendedState ?: ExtendedState()).copy(transformedData = transformedData)
                    }

                    propagateEvent<Event.Run>()
                }

                on<Event.Run> {
                    transitionTo(State.Loading)
                }
            }

            stateHandler<TransformedData, Unit>(State.Loading) {
                uponArrival {
                    extractInputFromExtendedState { it!!.transformedData!! }

                    execute { transformedData ->
                        println("uploading data...")
                        loadedData = transformedData
                    }

                    storeExecutionOutput { _, _ ->
                        ExtendedState()
                    }

                    propagateEvent<Event.Run>()
                }

                on<Event.Run> {
                    transitionTo(State.Waiting)
                }
            }
        }

        runBlocking {
            etl.processEvent(Event.Run)
        }

        loadedData shouldBe TransformedData(
            foo = "I'm foo!",
            bar = "I'm bar!"
        )

        with(etl.stateStore) {
            currentState shouldBe State.Waiting
            extendedState shouldBe ExtendedState()
        }
    }
}

inline fun <reified T> Gson.fromJson(jsonString: String): T = this.fromJson(jsonString, T::class.java)
