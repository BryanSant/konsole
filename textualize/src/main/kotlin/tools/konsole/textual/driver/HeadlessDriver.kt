package tools.konsole.textual.driver

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import tools.konsole.textual.events.Event

/**
 * In-memory [Driver] for tests and `Pilot` harnesses. Mirrors Python textual's
 * `HeadlessDriver`.
 *
 * Writes are captured into [output]; events are injected via [send] from test code.
 */
public class HeadlessDriver : Driver {

    private val eventChannel: Channel<Event> = Channel(Channel.UNLIMITED)
    private val outputBuffer: StringBuilder = StringBuilder()
    private var inputEnabled: Boolean = true

    override val events: Flow<Event> = eventChannel.consumeAsFlow()

    /** Accumulated terminal output. Tests can read this to assert rendered state. */
    public val output: String get() = outputBuffer.toString()

    /** Inject an [event] into the parsed-event stream. */
    public fun send(event: Event) {
        if (inputEnabled) eventChannel.trySend(event)
    }

    /** Clear the output buffer (e.g. between assertions). */
    public fun clearOutput() { outputBuffer.setLength(0) }

    override fun startApplicationMode() { /* nothing for headless */ }
    override fun stopApplicationMode() { eventChannel.close() }
    override fun suspendApplicationMode() { /* nothing */ }
    override fun resumeApplicationMode() { /* nothing */ }

    override fun write(data: String) { outputBuffer.append(data) }
    override fun flush() { /* StringBuilder doesn't need a flush */ }

    override fun disableInput() { inputEnabled = false }
    override fun enableInput() { inputEnabled = true }
}
