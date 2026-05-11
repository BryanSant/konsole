package tools.konsole.textual.worker

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Asynchronous task with a lifecycle distinct from raw coroutine [Job]s.
 * Mirrors Python textual's `Worker[T]` and `WorkerState`.
 *
 * Use [WorkerManager.run] from a [tools.konsole.textual.message.MessagePump] to
 * spawn workers tied to the pump's [CoroutineScope].
 */
public class Worker<T>(
    public val name: String,
    private val deferred: CompletableDeferred<T>,
    private val job: Job,
    private val stateRef: () -> WorkerState,
) {
    public val state: WorkerState get() = stateRef()
    public val isFinished: Boolean
        get() = state == WorkerState.Success || state == WorkerState.Error || state == WorkerState.Cancelled

    /** Cancel the worker. Idempotent. */
    public fun cancel() { job.cancel() }

    /** Suspend until the worker completes and return the result.
     *  Throws [WorkerFailed] / [WorkerCancelled] / [WorkerCancellationException] on non-Success. */
    public suspend fun await(): T = try { deferred.await() }
    catch (c: CancellationException) { throw WorkerCancelled(name, c) }
    catch (t: Throwable) { throw WorkerFailed(name, t) }
}

/** Worker lifecycle states — mirrors textual's `WorkerState` enum. */
public enum class WorkerState {
    Pending,
    Running,
    Cancelled,
    Error,
    Success,
}

public class WorkerError(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
public class WorkerFailed(name: String, cause: Throwable) : RuntimeException("Worker '$name' failed: ${cause.message}", cause)
public class WorkerCancelled(name: String, cause: Throwable? = null) : RuntimeException("Worker '$name' was cancelled", cause)

/**
 * Manages [Worker]s associated with a particular [CoroutineScope].
 *
 * A [tools.konsole.textual.message.MessagePump] typically owns one [WorkerManager];
 * call [run] to spawn a worker. The manager tracks active workers so an App can
 * await/cancel them on shutdown.
 */
public class WorkerManager(private val scope: CoroutineScope) {

    private val nextId = AtomicInteger(1)
    private val active: MutableList<Worker<*>> = mutableListOf()

    public fun <T> run(name: String? = null, block: suspend () -> T): Worker<T> {
        val workerName = name ?: "worker-${nextId.getAndIncrement()}"
        val deferred = CompletableDeferred<T>()
        var state = WorkerState.Pending
        val job = scope.launch {
            state = WorkerState.Running
            try {
                val result = block()
                state = WorkerState.Success
                deferred.complete(result)
            } catch (c: CancellationException) {
                state = WorkerState.Cancelled
                deferred.completeExceptionally(c)
                throw c
            } catch (t: Throwable) {
                state = WorkerState.Error
                deferred.completeExceptionally(t)
            }
        }
        // If the job is cancelled before launch starts running, the internal block's
        // CancellationException catch never runs. Track cancellation via invokeOnCompletion.
        job.invokeOnCompletion { cause ->
            if (cause is CancellationException && state != WorkerState.Success && state != WorkerState.Error) {
                state = WorkerState.Cancelled
                if (!deferred.isCompleted) deferred.completeExceptionally(cause)
            }
        }
        val w = Worker(workerName, deferred, job) { state }
        synchronized(active) { active += w }
        return w
    }

    public val activeCount: Int get() = synchronized(active) { active.count { !it.isFinished } }

    public fun cancelAll() {
        synchronized(active) {
            for (w in active) w.cancel()
        }
    }
}
