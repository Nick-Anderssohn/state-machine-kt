# State Machine Kt
A state machine library in kotlin

## Goals
* Easy to use, has an intuitive DSL.
* Fast and efficient, leverages coroutines
* Resilient and recoverable - can **safely** resume an execution 
    if cut short for some reason (i.e. power goes out).
* Out of the box drivers for popular data stores and other components
    such as Postgres and SQS.