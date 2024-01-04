package com.annakhuseinova

import kotlinx.coroutines.*
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
    logger.info("Water boiled")
}

suspend fun makeCoffee(){
    logger.info("Starting to make coffee")
    delay(500L)
    logger.info("Done with coffee")
}

suspend fun prepareAndReturnJavaCoffee(): String {
    logger.info("Starting to make coffee")
    delay(500L)
    logger.info("Done with coffee")
    return "Java coffee"
}

suspend fun prepareAndReturnToastedBread(): String {
    logger.info("Starting to make breakfast")
    delay(1000L)
    logger.info("Toast is out!")
    return "Toasted bread"
}

suspend fun sequentialMorningRoutine(){
    coroutineScope { // start a "context" for coroutines
        bathTime()
        // add more code including suspend functions
        // Inside coroutine scope all functions are executed sequentially (unless launched with "launch")
    }
    coroutineScope {
        boilingWater()
    }
}

suspend fun concurrentMorningRoutine(){
    coroutineScope {
        launch { // launch function allows to launch parallel execution of coroutines (like new Thread(...).start())
            bathTime()
        }
        // this coroutine is a CHILD of the coroutineScope. Can have nested child coroutines
        launch { boilingWater()
        }
    }
}

// Launching stuff in a GlobalScope.launch won't provide structured concurrency. Coroutines will run independently, won't
// have scope context
suspend fun noStructConcurrencyMorningRoutine(){
    GlobalScope.launch {
        bathTime()
    }
    GlobalScope.launch {
        boilingWater()
    }
}

// Example of launching a coroutine with the guarantee that the 2 previous once are completed
suspend fun morningRoutineWithCoffee(){
    coroutineScope {
        val bathTimeJob: Job = launch { bathTime() }
        val boilingWaterJob: Job = launch { boilingWater() }
        // join() is a semantic blocking method until the coroutine is done
        bathTimeJob.join()
        boilingWaterJob.join()
        launch {
            makeCoffee() // this function starts after bathTime() and boilingWater() finish
        }
    }
}


// structured concurrency allows to define in which order parallel computations will execute
suspend fun morningCoroutineWithCoffeeStructured(){
    coroutineScope {
        coroutineScope {
            launch { bathTime() }
            launch { boilingWater() }
        }
        launch { makeCoffee() }
    }
}

// Returning values from coroutines
suspend fun prepareBreakfast(){
    coroutineScope {
        val coffee: Deferred<String> = async { prepareAndReturnJavaCoffee() } // Deferred<*> contains the result of the computation
        // that will be filled once the computation is over. Analogous to Future in Java
        val toast: Deferred<String> = async { prepareAndReturnToastedBread() }

        // semantic blocking
        val finalCoffee = coffee.await()
        val finalToast = toast.await()
        logger.info("I'm eating $finalToast and drinking $finalCoffee")
    }
}

suspend fun main(args: Array<String>){
    prepareBreakfast()
}

