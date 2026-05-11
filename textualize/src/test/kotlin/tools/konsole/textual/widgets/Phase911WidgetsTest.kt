package tools.konsole.textual.widgets

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import tools.konsole.rich.Console
import tools.konsole.rich.RenderOptions
import tools.konsole.textual.binding.bindings
import tools.konsole.textual.compositor.Compositor

private fun render(w: tools.konsole.textual.widget.Widget, width: Int = 60): String {
    val console = Console.string(width = width)
    val opts = RenderOptions(maxWidth = width)
    return w.render().render(console, opts).joinToString("") { it.text }
}

class Phase911WidgetsTest : StringSpec({

    // ---- Placeholder ----

    "Placeholder renders its label inside a panel" {
        val p = Placeholder(label = "container", id = "sidebar")
        render(p, width = 40) shouldContain "container"
    }

    "Placeholder colors are deterministic for the same id" {
        val a = Placeholder(id = "abc")
        val b = Placeholder(id = "abc")
        render(a) shouldBe render(b)
    }

    // ---- ListItem ----

    "ListItem renders its child" {
        val item = ListItem(Label("nested"))
        render(item) shouldContain "nested"
    }

    "ListItem attaches the child to the DOM tree" {
        val child = Label("c")
        val item = ListItem(child)
        item.children.size shouldBe 1
        child.parent shouldBe item
    }

    // ---- Log ----

    "Log.write appends lines" {
        val log = Log()
        log.write("first")
        log.write("second")
        log.size shouldBe 2
    }

    "Log enforces maxLines FIFO" {
        val log = Log(maxLines = 2)
        log.write("a")
        log.write("b")
        log.write("c")
        log.size shouldBe 2
        val out = render(log)
        out shouldContain "b"
        out shouldContain "c"
    }

    "Log.write splits on newlines" {
        val log = Log()
        log.write("one\ntwo\nthree")
        log.size shouldBe 3
    }

    "Log doesn't markup-parse the input (plain text only)" {
        val log = Log()
        log.write("[bold]not bold[/]")
        val out = render(log)
        // The literal brackets survive (no markup expansion)
        out shouldContain "[bold]"
    }

    // ---- SelectionList ----

    "SelectionList tracks per-row selection state" {
        val sl = SelectionList(listOf(
            Triple("a", "Apple", false),
            Triple("b", "Banana", true),
            Triple("c", "Cherry", false),
        ))
        sl.selectedIds shouldBe setOf("b")
    }

    "SelectionList.toggleCurrent flips the highlighted row" {
        val sl = SelectionList(listOf(Triple("a", "A", false), Triple("b", "B", false)))
        sl.start()
        var lastSelection: Set<String>? = null
        sl.onMessage<SelectionList.SelectionChanged> { lastSelection = it.currentSelection }
        sl.toggleCurrent()
        runBlocking { delay(30); sl.stop() }
        sl.selectedIds shouldBe setOf("a")
        lastSelection shouldBe setOf("a")
    }

    "SelectionList.selectAll / .deselectAll affect every option" {
        val sl = SelectionList(listOf(Triple("a", "A", false), Triple("b", "B", false), Triple("c", "C", false)))
        sl.selectAll()
        sl.selectedIds shouldBe setOf("a", "b", "c")
        sl.deselectAll()
        sl.selectedIds.size shouldBe 0
    }

    "SelectionList.toggle(id) targets a specific option" {
        val sl = SelectionList(listOf(Triple("a", "A", true), Triple("b", "B", true)))
        sl.toggle("a")
        sl.selectedIds shouldBe setOf("b")
    }

    // ---- HelpPanel + KeyPanel ----

    "HelpPanel renders each binding key + description" {
        val map = bindings("q" to "quit", "ctrl+s" to "save")
        val help = HelpPanel(helpBindings = map)
        val out = render(help, width = 40)
        out shouldContain "q"
        out shouldContain "quit"
        out shouldContain "ctrl+s"
        out shouldContain "save"
    }

    "HelpPanel preferredLayer is POPUP" {
        val help = HelpPanel()
        help.preferredLayer shouldBe Compositor.POPUP
    }

    "KeyPanel renders a single binding chip" {
        val k = KeyPanel(tools.konsole.textual.binding.Binding(key = "ctrl+c", action = "cancel", description = "Cancel"))
        val out = render(k, width = 40)
        out shouldContain "ctrl+c"
        out shouldContain "Cancel"
    }

    // ---- MarkdownViewer ----

    "MarkdownViewer extracts headings into a table of contents" {
        val md = """
            |# Title
            |Body text.
            |## Section A
            |More body.
            |### Subsection
            |Detail.
            |## Section B
        """.trimMargin()
        val viewer = MarkdownViewer(markdown = md)
        viewer.tableOfContents.size shouldBe 4
        viewer.tableOfContents[0].title shouldBe "Title"
        viewer.tableOfContents[0].level shouldBe 1
        viewer.tableOfContents[2].title shouldBe "Subsection"
        viewer.tableOfContents[2].level shouldBe 3
    }

    "MarkdownViewer.update recomputes the TOC" {
        val viewer = MarkdownViewer(markdown = "# Original")
        viewer.tableOfContents.size shouldBe 1
        viewer.update("# A\n## B\n### C")
        viewer.tableOfContents.size shouldBe 3
    }

    "MarkdownViewer renders both TOC and body" {
        val viewer = MarkdownViewer(markdown = "# Hello\n\nBody text.")
        val out = render(viewer, width = 80)
        out shouldContain "Contents"
        out shouldContain "Hello"
    }

    "MarkdownViewer with showTableOfContents=false skips the sidebar" {
        val viewer = MarkdownViewer(markdown = "# x", showTableOfContents = false)
        val out = render(viewer, width = 80)
        out.contains("Contents") shouldBe false
    }
})
