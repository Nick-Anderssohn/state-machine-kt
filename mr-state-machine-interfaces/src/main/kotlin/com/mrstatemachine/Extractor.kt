package com.mrstatemachine

fun interface Extractor<TExtracted : Any, TExtendedState : Any> {
    fun extract(extendedState: TExtendedState?): TExtracted
}
