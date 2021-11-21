package com.mrstatemachine

fun interface Merger<in TIn : Any, TOut : Any> {
    suspend fun merge(input: TIn, destination: TOut?): TOut?
}

class NoOpMerger<in TIn : Any, TOut : Any> : Merger<TIn, TOut> {
    override suspend fun merge(input: TIn, destination: TOut?) = destination
}
