package com.mrstatemachine.engine

import com.mrstatemachine.Task

object NoOpProcessor : Task {
    override suspend fun run() {
        // Intentionally does nothing
    }
}
