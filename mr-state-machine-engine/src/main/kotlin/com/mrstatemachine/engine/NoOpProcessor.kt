package com.mrstatemachine.engine

import com.mrstatemachine.StateProcessor

object NoOpProcessor : StateProcessor {
    override suspend fun run() {
        // Intentionally does nothing
    }
}
