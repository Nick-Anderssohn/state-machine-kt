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
    fun `etl state machine runs completely`() {
        var loadedData: TransformedData? = null

        val etl = StateMachine<State, ExtendedState, Event> {
            startingState(State.Waiting)

            state<Unit, Unit>(State.Waiting) {
                on<Event.Run> {
                    transitionTo(State.Extracting)
                }
            }

            state<Unit, ExtractedData>(State.Extracting) {
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

            state<ExtractedData, TransformedData>(State.Transforming) {
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

            state<TransformedData, Unit>(State.Loading) {
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
    }
}

inline fun <reified T> Gson.fromJson(jsonString: String): T = this.fromJson(jsonString, T::class.java)
