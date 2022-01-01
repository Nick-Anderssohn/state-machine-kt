package com.statemachinekt.engine

@PublishedApi
internal inline fun <T> T.applyIf(condition: Boolean, fn: T.() -> Unit): T {
    if (condition) {
        this.fn()
    }

    return this
}

internal inline fun <T> T?.applyIfNotNull(fn: T.() -> Unit) = this?.apply(fn)
