package tools.konsole.examples

import tools.konsole.core.style.Color
import tools.konsole.rich.Renderable
import tools.konsole.rich.Segment
import tools.konsole.rich.Strip
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.geometry.Region
import tools.konsole.textual.app.App
import tools.konsole.textual.binding.bindings
import tools.konsole.textual.compositor.Compositor
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.driver.systemDriver
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.widgets.Footer
import tools.konsole.textual.widgets.Header
import tools.konsole.textual.widgets.Input

/**
 * Chat UI inspired by textual's `examples/mother.py`. The Python version
 * talks to a real LLM via the `llm` package; this port keeps the layout
 * faithful but stubs the responses with canned "Mother (from Alien)" lines —
 * a real LLM integration would replace [stubReply] with an HTTP call.
 *
 *   ./gradlew :examples:runExample -Pexample=MotherApp
 *
 * Type into the Input at the bottom and press Enter. `q` doesn't quit
 * (you'd never be able to type the letter); use Ctrl+C.
 */
public class MotherApp(headless: Boolean = false) : App(if (headless) HeadlessDriver() else systemDriver()) {

    private val header = Header(title = "Mother — interface 2037 ready for inquiry")
    private val chat = ChatView()
    private val input = Input(placeholder = "Type a question and press Enter…", id = "prompt")
    private val footer = Footer(bindings = bindings("Ctrl+C" to "quit"))

    override val bindings = bindings("ctrl+c" to "quit")

    init {
        input.start()
        input.onMessage<Input.Submitted> { evt ->
            val question = evt.value.trim()
            if (question.isNotEmpty()) {
                chat.addPrompt(question)
                input.clear()
                chat.addResponse(stubReply(question))
                requestRefresh()
            }
        }
    }

    override fun compose(): Sequence<Widget> = sequenceOf(header, chat, input, footer)

    override fun arrangeBaseLayer(widgets: List<Widget>) {
        compositor.placeAt(header, Region(0, 0, screenWidth, 1), Compositor.BASE)
        // Reserve top row for header, bottom two for input + footer.
        val chatHeight = (screenHeight - 4).coerceAtLeast(1)
        compositor.placeAt(chat, Region(0, 1, screenWidth, chatHeight), Compositor.BASE)
        compositor.placeAt(input, Region(0, screenHeight - 2, screenWidth, 1), Compositor.BASE)
        compositor.placeAt(footer, Region(0, screenHeight - 1, screenWidth, 1), Compositor.BASE)
        // Focus the input by default so typing works immediately.
        if (focused !== input) setFocus(input)
    }

    private fun stubReply(question: String): String {
        val canned = listOf(
            "Affirmative. The procedure has been logged.",
            "I'm afraid I can't comment on that, dear child.",
            "Bishop is aware of this. He will respond shortly.",
            "Standby. Cross-referencing crew quarters now…",
            "Special Order 937: priority one. Resolve at all costs.",
            "I have no further data on '$question'. Try again, please.",
        )
        return canned[(question.hashCode() and Int.MAX_VALUE) % canned.size]
    }
}

private class ChatView : Widget() {
    private val history: MutableList<Entry> = mutableListOf()

    fun addPrompt(text: String) { history += Entry(Role.User, text) }
    fun addResponse(text: String) { history += Entry(Role.Mother, text) }

    override fun render(): Renderable {
        val text = Text()
        val userStyle = Style(color = Color.Cyan, bold = true)
        val motherStyle = Style(color = Color.Green, bold = true)
        val bodyStyle = Style(color = Color.White)
        if (history.isEmpty()) {
            text.append("  INTERFACE 2037 READY FOR INQUIRY\n", motherStyle)
            return text
        }
        for (entry in history) {
            when (entry.role) {
                Role.User -> {
                    text.append("  you: ", userStyle); text.append(entry.body + "\n", bodyStyle)
                }
                Role.Mother -> {
                    text.append("  mother: ", motherStyle); text.append(entry.body + "\n", bodyStyle)
                }
            }
        }
        return text
    }

    override fun renderLine(y: Int, width: Int): Strip {
        val region = lastRegion ?: return Strip.EMPTY
        // Show most recent lines that fit in the region; auto-scroll behaviour.
        val flat = render().render(
            console = tools.konsole.rich.Console.string(width = width),
            options = tools.konsole.rich.RenderOptions(maxWidth = width),
        ).toList()
        val lines = mutableListOf<MutableList<Segment>>(mutableListOf())
        for (seg in flat) {
            if (seg.text == "\n") lines.add(mutableListOf())
            else lines.last().add(seg)
        }
        val total = lines.size
        val firstVisible = (total - region.height).coerceAtLeast(0)
        val absIdx = firstVisible + y
        val line = lines.getOrNull(absIdx) ?: return Strip.EMPTY
        return Strip.of(line).adjustCellLength(width)
    }

    private enum class Role { User, Mother }
    private data class Entry(val role: Role, val body: String)
}

public fun main() {
    MotherApp().run()
}
