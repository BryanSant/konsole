package tools.konsole.textual

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import tools.konsole.textual.worker.WorkerManager
import tools.konsole.textual.worker.WorkerState

class WorkerTest : StringSpec({

    "successful worker resolves with its result" {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val mgr = WorkerManager(scope)
        val w = mgr.run { 42 }
        runBlocking {
            w.await() shouldBe 42
            w.state shouldBe WorkerState.Success
            w.isFinished shouldBe true
        }
    }

    "cancelled worker transitions to Cancelled state" {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val mgr = WorkerManager(scope)
        val w = mgr.run<Int> {
            delay(5000)
            42
        }
        w.cancel()
        runBlocking { delay(100) }
        w.state shouldBe WorkerState.Cancelled
    }

    "failing worker transitions to Error" {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val mgr = WorkerManager(scope)
        val w = mgr.run<Int> { error("kaboom") }
        runBlocking { delay(100) }
        w.state shouldBe WorkerState.Error
    }
})
