package tools.konsole.textual.widgets

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import tools.konsole.rich.Console
import tools.konsole.rich.RenderOptions

private fun render(w: tools.konsole.textual.widget.Widget, width: Int = 60): String {
    val console = Console.string(width = width)
    val opts = RenderOptions(maxWidth = width)
    return w.render().render(console, opts).joinToString("") { it.text }
}

class Phase95WidgetsTest : StringSpec({

    // ---- Digits ----

    "Digits renders 0-9 as 5-row glyphs" {
        val d = Digits("01")
        val out = render(d, width = 80)
        out.lines().size shouldBe 5
    }

    "Digits.update changes the rendered value" {
        val d = Digits("0")
        d.update("12:34")
        d.value shouldBe "12:34"
    }

    // ---- Input ----

    "Input.insert appends at cursor and fires Changed" {
        val i = Input(initial = "")
        i.start()
        var last: String? = null
        i.onMessage<Input.Changed> { last = it.value }
        i.insert("hello")
        runBlocking { delay(30); i.stop() }
        i.value shouldBe "hello"
        last shouldBe "hello"
    }

    "Input.deleteLeft removes char before cursor" {
        val i = Input(initial = "hello")
        i.deleteLeft()
        i.value shouldBe "hell"
    }

    "Input.submit fires Submitted with final value" {
        val i = Input(initial = "go")
        i.start()
        var submitted: String? = null
        i.onMessage<Input.Submitted> { submitted = it.value }
        i.submit()
        runBlocking { delay(30); i.stop() }
        submitted shouldBe "go"
    }

    "Input.password mode renders dots" {
        val i = Input(initial = "secret", password = true)
        render(i) shouldContain "••••••"
    }

    "LengthValidator enforces min/max" {
        val v = LengthValidator(min = 3, max = 5)
        v.validate("hi").isValid shouldBe false
        v.validate("ok!").isValid shouldBe true
        v.validate("waytoolong").isValid shouldBe false
    }

    "RegexValidator passes matching values only" {
        val v = RegexValidator("\\d+")
        v.validate("123").isValid shouldBe true
        v.validate("12a").isValid shouldBe false
    }

    "IntegerValidator/NumberValidator pass numeric input" {
        IntegerValidator.validate("42").isValid shouldBe true
        IntegerValidator.validate("4.2").isValid shouldBe false
        NumberValidator.validate("3.14").isValid shouldBe true
    }

    "WordListSuggester returns the first prefix match" {
        val s = WordListSuggester(listOf("apple", "banana", "cherry", "apricot"))
        s.suggest("ap") shouldBe "apple"
        s.suggest("c") shouldBe "cherry"
        s.suggest("z") shouldBe null
    }

    // ---- RadioSet ----

    "RadioSet enforces single-selection invariant on init" {
        val rs = RadioSet(
            RadioButton("a", initial = true),
            RadioButton("b", initial = true),
            RadioButton("c"),
        )
        rs.buttons.count { it.value } shouldBe 1
    }

    "RadioSet.select flips exactly one and posts Changed" {
        val rs = RadioSet(RadioButton("a"), RadioButton("b"), RadioButton("c"))
        rs.start()
        var idx: Int? = null
        rs.onMessage<RadioSet.Changed> { idx = it.index }
        rs.select(2)
        runBlocking { delay(30); rs.stop() }
        rs.buttons.count { it.value } shouldBe 1
        rs.pressedIndex shouldBe 2
        idx shouldBe 2
    }

    // ---- Collapsible ----

    "Collapsible.toggle flips state and posts Toggled" {
        val c = Collapsible("Details", collapsed = true)
        c.start()
        var lastState: Boolean? = null
        c.onMessage<Collapsible.Toggled> { lastState = it.collapsed }
        c.toggle()
        runBlocking { delay(30); c.stop() }
        c.collapsed shouldBe false
        lastState shouldBe false
    }

    "Collapsible renders ▶ when collapsed and ▼ when expanded" {
        val c = Collapsible("Show me", collapsed = true)
        render(c) shouldContain "▶"
        c.toggle()
        render(c) shouldContain "▼"
    }

    // ---- OptionList ----

    "OptionList navigates and posts Highlighted/Selected" {
        val ol = OptionList.ofLabels(listOf("Apple", "Banana", "Cherry"))
        ol.start()
        var hl: Int? = null
        var sel: Int? = null
        ol.onMessage<OptionList.Highlighted> { hl = it.index }
        ol.onMessage<OptionList.Selected> { sel = it.index }
        ol.moveCursor(1)
        ol.selectCurrent()
        runBlocking { delay(30); ol.stop() }
        hl shouldBe 1
        sel shouldBe 1
    }

    "OptionList renders ▶ cursor at highlighted index" {
        val ol = OptionList.ofLabels(listOf("a", "b", "c"))
        ol.moveCursor(1)
        val out = render(ol)
        out shouldContain "▶ b"
    }

    // ---- Select ----

    "Select.setSelected updates the displayed label and posts Changed" {
        val s = Select(listOf("k" to "Kotlin", "j" to "Java", "p" to "Python"))
        s.start()
        var captured: String? = null
        s.onMessage<Select.Changed<*>> { captured = it.value as? String }
        s.setSelected("p")
        runBlocking { delay(30); s.stop() }
        s.selected shouldBe "p"
        s.selectedLabel shouldBe "Python"
        captured shouldBe "p"
    }

    // ---- ListView ----

    "ListView navigates between widget items" {
        val lv = ListView(listOf(Label("a"), Label("b"), Label("c")))
        lv.start()
        var hl: Int? = null
        lv.onMessage<ListView.Highlighted> { hl = it.index }
        lv.moveCursor(2)
        runBlocking { delay(30); lv.stop() }
        hl shouldBe 2
    }

    // ---- Tree ----

    "Tree builds a node hierarchy and reports current node" {
        val t = Tree<String>("root", rootData = "/")
        val src = t.root.addLeaf("src", "/src")
        src.addLeaf("main.kt", "/src/main.kt")
        src.addLeaf("util.kt", "/src/util.kt")
        t.root.addLeaf("README.md", "/README.md")
        t.root.children.size shouldBe 2
    }

    "Tree.moveCursor walks visible nodes" {
        val t = Tree<Int>("root")
        t.root.addLeaf("a")
        t.root.addLeaf("b")
        t.root.expanded = true
        t.start()
        t.moveCursor(1)
        kotlinx.coroutines.runBlocking { delay(20); t.stop() }
        t.currentNode()?.label shouldBe "a"
    }

    // ---- Markdown widget ----

    "Markdown widget renders Markdown source" {
        val m = Markdown("# Hello\n\nThis is **bold**.")
        render(m, width = 60) shouldContain "Hello"
    }

    "Markdown.update replaces content" {
        val m = Markdown("first")
        m.update("# second")
        m.markdown shouldContain "second"
    }

    // ---- RichLog ----

    "RichLog.write appends a line" {
        val log = RichLog()
        log.write("first line")
        log.write("second line")
        log.size shouldBe 2
    }

    "RichLog enforces maxLines FIFO" {
        val log = RichLog(maxLines = 2)
        log.write("a")
        log.write("b")
        log.write("c")
        log.size shouldBe 2
        render(log) shouldContain "b"
        render(log) shouldContain "c"
    }

    "RichLog.clear empties the buffer" {
        val log = RichLog()
        log.write("dirty")
        log.clear()
        log.size shouldBe 0
    }
})
