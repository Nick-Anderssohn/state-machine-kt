package com.mrstatemachine.engine

internal class StateStore<TStateBase : Any, TExtendedState : Any>(
    @Volatile
    var currentState: TStateBase,

    @Volatile
    var extendedState: TExtendedState?,

//    private val mergers: Map<Class<*>, Merger<*, TExtendedState>>,
//
//    private val extractors: Map<Class<*>, Extractor<*, TExtendedState>>
) {
//    inline fun <reified TIn : Any> mergeIntoExtendedState(input: TIn) {
//        mergeIntoExtendedState(TIn::class.java, input)
//    }
//
//    @PublishedApi
//    internal fun <TIn : Any> mergeIntoExtendedState(clazz: Class<TIn>, input: TIn) {
//        require(clazz in mergers) { "no merger defined for input type $clazz" }
//
//        val merger = mergers[clazz]!! as Merger<TIn, TExtendedState>
//
//        extendedState = merger.merge(input, extendedState)
//    }
//
//    inline fun <reified TExtracted : Any> extractFromExtendedState(): TExtracted {
//        return extractFromExtendedState(TExtracted::class.java)
//    }
//
//    @PublishedApi
//    internal fun <TExtracted : Any> extractFromExtendedState(clazz: Class<TExtracted>): TExtracted {
//        require(clazz in extractors) { "no extractor defined for type $clazz" }
//
//        return (extractors[clazz]!! as Extractor<TExtracted, TExtendedState>).extract(extendedState)
//    }
}
