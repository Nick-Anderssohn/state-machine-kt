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
        object ExtractionSuccessful : Event()
        object TransformationSuccessful : Event()
        object LoadingSuccessful : Event()
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
            startingExtendedState(ExtendedState())

            stateDefinition(State.Waiting) {
                on<Event.Run> {
                    transitionTo(State.Extracting)

                    execute { _ -> ExtendedState() }
                }
            }

            stateDefinition(State.Extracting) {
                uponArrival {
                    execute { _, _ ->
                        println("retrieving data...")
                        val extractedData = ExtractedData(raw = "{\"foo\": \"I'm foo!\",\"bar\": \"I'm bar!\"}")

                        ActionResult(
                            extendedState = ExtendedState(extractedData = extractedData),
                            eventToTrigger = Event.ExtractionSuccessful
                        )
                    }
                }

                on<Event.ExtractionSuccessful> {
                    transitionTo(State.Transforming)
                }
            }

            stateDefinition(State.Transforming) {
                uponArrival {
                    execute { _, extendedStateStore ->
                        val extendedState = extendedStateStore.extendedState

                        println("transforming data...")
                        val transformedData = gson.fromJson<TransformedData>(extendedState.extractedData!!.raw)

                        ActionResult(
                            extendedState = extendedState.copy(transformedData = transformedData),
                            eventToTrigger = Event.TransformationSuccessful
                        )
                    }
                }

                on<Event.TransformationSuccessful> {
                    transitionTo(State.Loading)
                }
            }

            stateDefinition(State.Loading) {
                uponArrival {
                    execute { _, extendedStateStore ->
                        val extendedState = extendedStateStore.extendedState

                        println("uploading data...")
                        loadedData = extendedState.transformedData

                        ActionResult(
                            extendedState = extendedState,
                            eventToTrigger = Event.LoadingSuccessful
                        )
                    }
                }

                on<Event.LoadingSuccessful> {
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

        etl.currentState shouldBe State.Waiting
        etl.currentExtendedState shouldBe ExtendedState(
            extractedData = ExtractedData(raw = "{\"foo\": \"I'm foo!\",\"bar\": \"I'm bar!\"}"),
            transformedData = TransformedData(
                foo = "I'm foo!",
                bar = "I'm bar!"
            )
        )
    }
}

inline fun <reified T> Gson.fromJson(jsonString: String): T = this.fromJson(jsonString, T::class.java)
