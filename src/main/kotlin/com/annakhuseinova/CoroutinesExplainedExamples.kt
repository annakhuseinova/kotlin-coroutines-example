package com.annakhuseinova

import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

/*
* Coroutine - user-space thread, "lightweight thread", "thread on top of OS thread", computation that can be suspended
* and resumed at any time.
* Coroutine is run via a function with "suspend" keyword. A suspend function can be called only from a coroutine or
* another suspend function. It can be suspended and resumed.
* Continuation is a data structure that stores all local context of execution - local variables, point of execution.
* Continuation is an object of type Continuation<T>. The T type variable represents the function's return type. The
* continuation contains all the state of the variables and parameters of the function. It also includes the label that
* stores the point where the execution was suspended.
*
* -Dkotlinx.coroutines.debug - VM property that allows to show coroutine id
* */

val logger: Logger = LoggerFactory.getLogger("CoroutinesPlayground")

// Some util functions for demo purposes

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

suspend fun workingHard(){
    logger.info("Working")

    // kinda CU-intensive computation
    while (true){
        // do some hard code
    }
    delay(100L)
    logger.info("Work done")
}

suspend fun workingNicely(){
    logger.info("Working")

    // kinda CU-intensive computation
    while (true){
        delay(100L) // give the dispatcher the chance to run another coroutine
    }
    delay(100L)
    logger.info("Work done")
}

suspend fun takeABreak(){
    logger.info("Taking a break")
    delay(1000L)
    logger.info("Break done")
}

suspend fun drinkingWater(){
    while (true){
        logger.info("Drinking water")
        delay(1000L)
    }
}


// 1. Execution within coroutineScope without "launch" function - this way the execution will be sequential
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

// 2. Execution within "launch" functions is parallel. coroutineScope will block until all coroutines inside finish
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
/*
* 3. Launching stuff in a GlobalScope.launch won't provide structured concurrency. Coroutines will run independently,
*  They won't have scope context
* */
suspend fun noStructConcurrencyMorningRoutine(){
    GlobalScope.launch {
        bathTime()
    }
    GlobalScope.launch {
        boilingWater()
    }
}

/*
* 4. Example of launching a coroutine with the guarantee that the 2 previous once are completed
* "launch" function returns a Job instance on which we can call join() (wait for the completion) and cancel()(send
* cancellation signal to the coroutine)
* */
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

/*
* 5.Structured concurrency allows to define in which order parallel computations will execute. "coroutineScope@
* function will block until all coroutines inside it finish
 */

suspend fun morningCoroutineWithCoffeeStructured(){
    coroutineScope {
        coroutineScope {
            launch { bathTime() }
            launch { boilingWater() }
        }
        launch { makeCoffee() }
    }
}

/*
* 6. "async" builder function allows to return values from coroutines.
* It returns Deferred<*> object which is like a Java Future
* To wait for the value - call await() on the Deferred instance
* */
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

/*
* 7. Cooperative scheduling - coroutines yield manually. They yield at the yielding point which is usually the suspend function
* If there is no suspend function call - IT WILL NOT YIELD
* */
suspend fun workHardRoutine(){
    // Thus, we provide our own implementation of thread pool for dispatcher use
    val dispatcher: CoroutineDispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()
    coroutineScope {
        launch(dispatcher) {
            workingHard()
        }
        launch(dispatcher) {
            takeABreak()
        }
    }
}

suspend fun workNicelyRoutine(){
    // Thus, we provide our own implementation of thread pool for dispatcher use
    val dispatcher: CoroutineDispatcher = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
    coroutineScope {
        launch(dispatcher) {
            workingNicely()
        }
        launch(dispatcher) {
            takeABreak()
        }
    }
}

/*
* 8. Dispatchers are basically thread pools used to run coroutines
* */
val simpleDispatcher = Dispatchers.Default // "normal code" = short code or yielding coroutines. Option 1
val blockingDispatcher = Dispatchers.IO // blocking code (e.g. DB connections, long-running computations). Option 2
val customerDispatcher = Executors.newFixedThreadPool(8).asCoroutineDispatcher() // on top of your own thread pool. Option 3

/*
* 9. Cancellation of coroutines. If a Job is canceled,  a CancellationException will be thrown.
* When a child coroutine throws an exception, the parent coroutine is also stopped.
* */
suspend fun forgettingFriendBirthdayRoutine(){
    coroutineScope {
        val workingJob: Job = launch {
            launch {
                workingNicely()
            }
            launch {
                drinkingWater()
            }
        }
        launch {
            delay(2000L) // after 2s I remember I have a birthday today
            workingJob.cancel() // send a SIGNAL to the coroutine to cancel, cancellation happens at first yielding point
            // if the coroutine does not have a yielding point - it will not be cancelled
            workingJob.join() // you are sure that the coroutine has been cancelled
            logger.info("I forgot my friend's birthday! Buying a present now!")
        }

    }
}

/*
* 10. If a coroutine doesn't yield, it can't be cancelled
* */
suspend fun forgettingFriendBirthdayRoutineUncancellable(){
    coroutineScope {
        val workingJob: Job = launch {
            workingHard()
        }
        launch {
            delay(2000L) // after 2s I remember I have a birthday today
            logger.info("Trying to stop working")
            workingJob.cancel() // send a SIGNAL to the coroutine to cancel, cancellation happens at first yielding point
            // if the coroutine does not have a yielding point - it will not be cancelled (NEVER)
            workingJob.join() // semantic blocking, but the coroutine will never stop execution
            logger.info("I forgot my friend's birthday! Buying a present now!")
        }

    }
}

class Desk: AutoCloseable {

    init {
        logger.info("Starting to work on this desk")
    }

    override fun close() {
        logger.info("Cleaning up the desk")
    }
}

/*
* 11. Correct cancellation and closure of resources inside coroutines is extremely important!
* */
suspend fun forgettingFriendBirthdayRoutineUncancellableWithResource(){
    val desk = Desk()
    coroutineScope {
        val workingJob: Job = launch {
            desk.use { _ -> // this resource will be closed upon completion of the coroutine
                workingHard()
            }
        }
        // can also define your own "cleanup" code in case of completion or cancellation
        workingJob.invokeOnCompletion { exception: Throwable? ->
            // can handle completion and cancellation differently, depending on the exception
            logger.info("Make sure I talk to my colleagues that I'll be out for 30 mins")
         }
        launch {
            delay(2000L) // after 2s I remember I have a birthday today
            logger.info("Trying to stop working")
            workingJob.cancel() // send a SIGNAL to the coroutine to cancel, cancellation happens at first yielding point
            // if the coroutine does not have a yielding point - it will not be cancelled (NEVER)
            workingJob.join() // semantic blocking, but the coroutine will never stop execution
            logger.info("I forgot my friend's birthday! Buying a present now!")
        }

    }
}

/*
* 12. Cancellation of a parent coroutine propagates to child coroutines - they will be also cancelled
* */
suspend fun forgettingFriendBirthdayRoutineStayHydrated(){
    coroutineScope {
        val workingJob: Job = launch {
            workingNicely()
        }
        launch {
            delay(2000L) // after 2s I remember I have a birthday today
            workingJob.cancel() // send a SIGNAL to the coroutine to cancel, cancellation happens at first yielding point
            // if the coroutine does not have a yielding point - it will not be cancelled
            workingJob.join() // you are sure that the coroutine has been cancelled
            logger.info("I forgot my friend's birthday! Buying a present now!")
        }

    }
}

/*
* 13.CoroutineContext is a way to store information passed from parents to children to develop structural concurrency internally.
* It contains the name of the coroutine, the dispatcher (aka, the pool of threads executing the coroutines),
* the exception handler, and so on.
* It represents a collection of elements, but also, every element is a collection.
* The CoroutineScope retains a reference to a coroutine context.
* */
suspend fun asynchronousGreeting(){
    coroutineScope {
        launch(CoroutineName("Greeting Coroutine" + Dispatchers.Default)) {
            logger.info("Hello, everyone!")
        }
    }
}

/*
 * 14. Any parent coroutine gives its context to its children coroutines. Children can override that context
 * */
suspend fun demoContextInheritance(){
    coroutineScope {
        launch(CoroutineName("Greeting Coroutine")) {
            logger.info("[parent coroutine] Hello!")
            launch(CoroutineName("Child Greeting Coroutine")) {
                // coroutine context will be inherited here. You may override it. Overriding
                // will not affect the parent CoroutineContext
                logger.info("[child coroutine] Hi there!")
            }
            delay(200)
            logger.info("[parent coroutine] Hi again from parent!")
        }
    }
}

suspend fun main(args: Array<String>){
    workNicelyRoutine()
}

