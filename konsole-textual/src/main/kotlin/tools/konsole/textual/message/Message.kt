package tools.konsole.textual.message

import kotlinx.coroutines.CompletableDeferred

/**
 * Base class for all messages flowing through a [MessagePump].
 * Mirrors Python textual's `Message`.
 *
 * Messages **bubble** by default — they are dispatched to the originating node,
 * then propagate up the DOM tree until they reach the App. Either side can call
 * [stop] to halt propagation or [preventDefault] to suppress the default action.
 *
 * Subclass to define your own messages. Typically a `data class`:
 *
 * ```
 * data class Submit(val payload: String) : Message()
 * ```
 *
 * Messages produced by built-in widgets ship in [tools.konsole.textual.widget.messages].
 */
public open class Message {

    /** Whether [stop] has been called — message will not propagate further. */
    @Volatile public var isStopped: Boolean = false
        private set

    /** Whether [preventDefault] has been called — default handler should be skipped. */
    @Volatile public var isDefaultPrevented: Boolean = false
        private set

    /** Whether the message has been forwarded back to its origin. Used by some handlers. */
    @Volatile public var isHandled: Boolean = false

    /** Completion signal — resolved once dispatch has finished traversing the tree. */
    public val dispatched: CompletableDeferred<Unit> = CompletableDeferred()

    /** Halt further bubbling of this message. */
    public fun stop() {
        isStopped = true
    }

    /** Suppress the default action associated with this message. */
    public fun preventDefault() {
        isDefaultPrevented = true
    }
}
