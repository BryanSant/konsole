package tools.konsole.textual.message

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * Dispatches [Message]s to handlers. Mirrors Python textual's `MessagePump`.
 *
 * Each pump owns an unbounded mailbox [Channel] and a consumer coroutine that
 * drains messages on [Dispatchers.Default]. Handlers are looked up by message
 * class via [registerHandler] or installed directly via [onMessage].
 *
 * Lifecycle:
 *   ```
 *   val pump = MyPump(scope = CoroutineScope(SupervisorJob() + Dispatchers.Default))
 *   pump.start()
 *   pump.post(MyMessage())
 *   …
 *   pump.stop()
 *   ```
 *
 * Subclasses (Widget, Screen, App) override [onEvent] to react to lifecycle/input
 * events before they propagate via bubbling.
 */
public open class MessagePump(
    public val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {

    // Bounded so a stuck consumer can't grow the heap without limit.
    // post() uses trySend, so overflow surfaces as `false` rather than
    // suspending the producer (and risking deadlock if producer == consumer).
    private val mailbox: Channel<Message> = Channel(capacity = MAILBOX_CAPACITY)
    private val handlers: MutableMap<KClass<out Message>, MutableList<suspend (Message) -> Unit>> = mutableMapOf()
    private var consumerJob: Job? = null

    /** Whether the pump is currently consuming. */
    public val isRunning: Boolean get() = consumerJob?.isActive == true

    /** Begin draining the mailbox. Idempotent. */
    public open fun start() {
        if (consumerJob?.isActive == true) return
        consumerJob = scope.launch {
            mailbox.consumeEach { msg ->
                try {
                    dispatch(msg)
                } finally {
                    if (!msg.dispatched.isCompleted) msg.dispatched.complete(Unit)
                }
            }
        }
    }

    /** Stop consuming and cancel the consumer coroutine. */
    public open fun stop() {
        mailbox.close()
        consumerJob?.cancel()
    }

    /** Post a message to this pump's mailbox. Non-blocking. */
    public fun post(message: Message): Boolean = mailbox.trySend(message).isSuccess

    /** Register a handler that fires for messages of (or subclassing) [type]. */
    @Suppress("UNCHECKED_CAST")
    public fun <M : Message> registerHandler(type: KClass<M>, handler: suspend (M) -> Unit) {
        handlers.getOrPut(type) { mutableListOf() }.add(handler as suspend (Message) -> Unit)
    }

    /** Convenience: `onMessage<MyMsg> { … }`. */
    public inline fun <reified M : Message> onMessage(noinline handler: suspend (M) -> Unit) {
        registerHandler(M::class, handler)
    }

    /**
     * Override to intercept events before generic dispatch. Default is no-op.
     *
     * Public so the App can deliver Key events to the focused widget
     * synchronously, preserving order against binding-action dispatch.
     */
    public open suspend fun onEvent(event: tools.konsole.textual.events.Event) {}

    /** Schedule [block] to run after [millis]. Returns the launched [Job] so it can be cancelled. */
    public fun setTimer(millis: Long, block: suspend () -> Unit): Job = scope.launch {
        delay(millis)
        if (isActive) block()
    }

    /** Schedule [block] to run repeatedly every [millis] until the returned [Job] is cancelled. */
    public fun setInterval(millis: Long, block: suspend () -> Unit): Job = scope.launch {
        while (isActive) {
            delay(millis)
            if (!isActive) break
            block()
        }
    }

    public companion object {
        /**
         * Per-pump mailbox capacity. Large enough that input bursts don't drop
         * events under normal load, small enough that a stuck handler can't
         * exhaust the heap. Hit it and you have an upstream bug worth finding.
         */
        public const val MAILBOX_CAPACITY: Int = 1024
    }

    private suspend fun dispatch(message: Message) {
        if (message is tools.konsole.textual.events.Event) {
            try { onEvent(message) } catch (_: Throwable) { /* swallow — textual logs */ }
            if (message.isStopped) return
        }
        // Match handlers by exact class then by superclass — covers data-class messages
        // dispatched against open base types.
        val matched = handlers.entries.filter { (k, _) -> k.isInstance(message) }
        for ((_, handlers) in matched) {
            for (h in handlers) {
                try { h(message) } catch (_: Throwable) { /* swallow */ }
                if (message.isStopped) return
            }
        }
    }
}

