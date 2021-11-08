package com.mrstatemachine

interface Task {
    suspend fun run(): Any
}
