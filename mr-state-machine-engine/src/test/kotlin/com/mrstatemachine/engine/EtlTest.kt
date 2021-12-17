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

        val etl = StateMachine<State, ExtendedState, Event> {
            startingState(State.Waiting)

            stateHandler(State.Waiting) {
                on<Event.Run> {
                    transitionTo(State.Extracting)
                }
            }

            stateHandler(State.Extracting) {
                uponArrival {
                    execute { _, _ ->
                        println("retrieving data...")
                        val extractedData = ExtractedData(raw = "{\"foo\": \"I'm foo!\",\"bar\": \"I'm bar!\"}")
                        ExtendedState(extractedData = extractedData)
                    }
                }
            }
                .then<Event.Run>(State.Transforming) {
                    uponArrival {
                        execute { _, extendedStateStore ->
                            val extendedState = extendedStateStore.extendedState

                            println("transforming data...")
                            val transformedData = gson.fromJson<TransformedData>(extendedState.extractedData!!.raw)

                            extendedState.copy(transformedData = transformedData)
                        }
                    }
                }
                .then<Event.Run>(State.Loading) {
                    uponArrival {
                        execute { _, extendedStateStore ->
                            val extendedState = extendedStateStore.extendedState

                            println("uploading data...")
                            loadedData = extendedState.transformedData

                            extendedState
                        }
                    }
                }
                .then<Event.Run>(State.Waiting)
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
            extendedStateStore.extendedState shouldBe ExtendedState(
                extractedData = ExtractedData(raw = "{\"foo\": \"I'm foo!\",\"bar\": \"I'm bar!\"}"),
                transformedData = TransformedData(
                    foo = "I'm foo!",
                    bar = "I'm bar!"
                )
            )
        }
    }

    @Test
    fun `etl state machine runs completely when defined using propagateEvent`() {
        var loadedData: TransformedData? = null

        val etl = StateMachine<State, ExtendedState, Event> {
            startingState(State.Waiting)

            stateHandler(State.Waiting) {
                on<Event.Run> {
                    transitionTo(State.Extracting)
                }
            }

            stateHandler(State.Extracting) {
                uponArrival {
                    execute { _, _ ->
                        println("retrieving data...")
                        ExtendedState(
                            extractedData = ExtractedData(raw = "{\"foo\": \"I'm foo!\",\"bar\": \"I'm bar!\"}")
                        )
                    }

                    propagateEvent<Event.Run>()
                }

                on<Event.Run> {
                    transitionTo(State.Transforming)
                }
            }

            stateHandler(State.Transforming) {
                uponArrival {
                    execute { _, extendedStateStore ->
                        println("transforming data...")
                        extendedStateStore.extendedState.copy(
                            transformedData = gson.fromJson<TransformedData>(
                                extendedStateStore.extendedState.extractedData!!.raw
                            )
                        )
                    }

                    propagateEvent<Event.Run>()
                }

                on<Event.Run> {
                    transitionTo(State.Loading)
                }
            }

            stateHandler(State.Loading) {
                uponArrival {
                    execute { _, extendedStateStore ->
                        println("uploading data...")
                        loadedData = extendedStateStore.extendedState.transformedData
                        extendedStateStore.extendedState
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
            extendedStateStore.extendedState shouldBe ExtendedState(
                extractedData = ExtractedData(raw = "{\"foo\": \"I'm foo!\",\"bar\": \"I'm bar!\"}"),
                transformedData = TransformedData(
                    foo = "I'm foo!",
                    bar = "I'm bar!"
                )
            )
        }
    }
}

inline fun <reified T> Gson.fromJson(jsonString: String): T = this.fromJson(jsonString, T::class.java)
