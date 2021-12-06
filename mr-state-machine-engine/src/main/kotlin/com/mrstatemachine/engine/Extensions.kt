package com.mrstatemachine.engine

@PublishedApi
internal inline fun <T> T.applyIf(condition: Boolean, fn: T.() -> Unit): T {
    if (condition) {
        this.fn()
    }

    return this
}
