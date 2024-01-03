package com.annakhuseinova

import kotlinx.coroutines.delay
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// Coroutine - user-space thread, "lightweight thread", "thread on top of OS thread", computation that can be suspended and resumed at any time.
// Coroutine is run via a function with "suspend" keyword
// Continuation is a data structure that stores all local context of execution - local variables, point of execution.
// Continuation is an object of type Continuation<T>. The T type variable represents the function's return type. The
// continuation contains all the state of the variables and parameters of the function. It also includes the label that
// stores the point where the execution was suspended.
// A suspend function can be called only from a coroutine or another suspend function. It can be suspended and resumed
val logger: Logger = LoggerFactory.getLogger("CoroutinesPlayground")
suspend fun bathTime(){
    logger.info("Going to the bathroom")
    // Continuation suspended
    delay(500L) // suspends ("blocks") the computation
    // Continuation restored
    logger.info("Bath done, exiting")
}

suspend fun boilingWater(){
    logger.info("Boiling water")
    delay(1000L)
    logger.info("Water bolied")
}

suspend fun main(args: Array<String>){
    bathTime()
}