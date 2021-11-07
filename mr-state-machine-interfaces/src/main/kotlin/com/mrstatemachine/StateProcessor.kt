package com.mrstatemachine

interface StateProcessor {
    suspend fun run(): Any
}
