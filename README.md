# State Machine Kt
A state machine library in kotlin. Great for both backend (e.x. a service or worker)
and frontend (e.x. android).

## Examples
Check out [the tests folder](./state-machine-kt-engine/src/test/kotlin/com/statemachinekt/engine) for examples of how to use this library.

## Goals
* Easy to use, has an intuitive DSL.
* Fast and efficient, leverages coroutines
* Optional persistence layer.
  * Resilient and recoverable - can **safely** resume an execution
    if it is cut short for some reason (e.x. power goes out).
  * Out of the box drivers for popular data stores such as Postgres.

## Roadmap
### State Machine Engine and DSL
#### UML-based Features
This portion of the roadmap contains a lot of links to the Wikipedia page on UML state
machines because it is very well written (as of 01/01/2022). I've backed up the
Wikipedia page in its current state in case someone hops on Wikipedia and screws it up.

- [x] [Events](https://en.wikipedia.org/wiki/UML_state_machine#Events)
- [x] [States](https://en.wikipedia.org/wiki/UML_state_machine#States)
- [x] [Extended state](https://en.wikipedia.org/wiki/UML_state_machine#Extended_states)
- [x] [Transition tasks](https://en.wikipedia.org/wiki/UML_state_machine#Actions_and_transitions) (AKA "Actions and transitions")
  - [x] Transition tasks can determine the target state (which state to transition to) *during* execution.
  - [x] [Guard conditions](https://en.wikipedia.org/wiki/UML_state_machine#Guard_conditions) - You
    can implement guards by performing checks within the transition task and allowing the task
    to determine which state to transition to.
- [x] [Entry actions](https://en.wikipedia.org/wiki/UML_state_machine#Orthogonal_regions)
- [x] [Exit actions](https://en.wikipedia.org/wiki/UML_state_machine#Orthogonal_regions)
- [ ] [Hierarchically nested states](https://en.wikipedia.org/wiki/UML_state_machine#Hierarchically_nested_states)
  - Note to self: I sort of accidentally implemented this to an extent with the
    `applyToAllStateDefinitions`/super vertex I added. Will create a more generalized solution
    for creating a hierarchy of state machines.
  - Note to self: might need interface for state machine...also might not until orthogonal regions tho.
- [ ] [Orthogonal regions](https://en.wikipedia.org/wiki/UML_state_machine#Orthogonal_regions)
  - Note to self: region key. Might need interface for state machine.
- [ ] [Event queue](https://en.wikipedia.org/wiki/UML_state_machine#Run-to-completion_execution_model)
- [ ] [Event deferral](https://en.wikipedia.org/wiki/UML_state_machine#Event_deferral)
- [ ] [Internal transitions](https://en.wikipedia.org/wiki/UML_state_machine#Internal_transitions)

### Persistence Layer
- [ ] Interface-based API for the persistence layer as its own module
- [ ] Potentially separate interface-based API for any queuing portions
- [ ] Postgres implementation as its own module
- [ ] Maybe SQS for any queuing portions

### Example Service
To test the usability of the library, I'll make a test backend for a generic marketplace
that includes cart, checkout, charge user, inventory modification, refund, etc...might actually
turn that into a library as well in case anyone wants to use it for a real marketplace. Once this
service is complete, then I'll mark the library's version as 1.0 and the API will be stable - semantic
versioning rules will be followed.
