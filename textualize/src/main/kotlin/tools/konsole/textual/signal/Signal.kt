package tools.konsole.textual.signal

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Type-safe pub/sub primitive. Mirrors Python textual's `Signal`.
 *
 * Subscribers are notified via [publish] in subscription order. Subscriptions
 * are weakly held by [Subscription] objects: dropping the reference does NOT
 * auto-unsubscribe (mirrors textual's semantics — callers must call
 * [Subscription.disconnect]).
 */
public class Signal<T> {

    private val subscribers: MutableList<(T) -> Unit> = CopyOnWriteArrayList()

    /** Subscribe a [handler] and return a disconnectable token. */
    public fun subscribe(handler: (T) -> Unit): Subscription {
        subscribers += handler
        return Subscription { subscribers.remove(handler) }
    }

    /** Notify all subscribers with [data]. Subscribers are called synchronously, in subscription order. */
    public fun publish(data: T) {
        for (sub in subscribers) {
            try { sub(data) } catch (_: Throwable) { /* swallow — one bad handler shouldn't block others */ }
        }
    }

    /** Drop all subscribers. */
    public fun clear() {
        subscribers.clear()
    }

    public val subscriberCount: Int get() = subscribers.size

    /** Token returned by [Signal.subscribe]. Call [disconnect] to stop receiving notifications. */
    public class Subscription internal constructor(private val disconnectFn: () -> Unit) {
        public fun disconnect() { disconnectFn() }
    }
}
