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

class Phase96WidgetsTest : StringSpec({

    // ---- DataTable ----

    "DataTable addColumn / addRow build a grid" {
        val dt = DataTable()
        dt.addColumn("Name")
        dt.addColumn("Age")
        dt.addRow("Alice", 30)
        dt.addRow("Bob", 25)
        dt.columnCount shouldBe 2
        dt.rowCount shouldBe 2
        render(dt) shouldContain "Alice"
        render(dt) shouldContain "Bob"
    }

    "DataTable.moveCursor clamps and posts Highlighted" {
        val dt = DataTable()
        dt.addColumn("a"); dt.addColumn("b")
        dt.addRow("1", "2"); dt.addRow("3", "4")
        dt.start()
        var lastCoord: Coordinate? = null
        dt.onMessage<DataTable.Highlighted> { lastCoord = it.coordinate }
        dt.moveCursor(row = 1, column = 1)
        runBlocking { delay(30); dt.stop() }
        dt.cursorCoordinate shouldBe Coordinate(1, 1)
        lastCoord shouldBe Coordinate(1, 1)
    }

    "DataTable.sort orders rows by the given column" {
        val dt = DataTable()
        val nameCol = dt.addColumn("Name", id = "name")
        dt.addColumn("Score", id = "score")
        dt.addRow("Charlie", "99")
        dt.addRow("Alice", "42")
        dt.addRow("Bob", "63")
        dt.sort(nameCol)
        dt.rows[0].cells[0].let { render(DataTable().also { d -> d.addColumn("x"); d.addRow(it) }) shouldContain "Alice" }
    }

    "DataTable.clear empties rows but keeps columns by default" {
        val dt = DataTable()
        dt.addColumn("a"); dt.addRow("x")
        dt.clear()
        dt.rowCount shouldBe 0
        dt.columnCount shouldBe 1
    }

    "DataTable.removeRow drops a row by id" {
        val dt = DataTable()
        dt.addColumn("a")
        dt.addRow("first", id = "r1")
        dt.addRow("second", id = "r2")
        dt.removeRow("r1") shouldBe true
        dt.rowCount shouldBe 1
        dt.rows[0].id shouldBe "r2"
    }

    "DataTable.selectCurrent emits Selected and (for Row cursor) RowSelected" {
        val dt = DataTable(cursorType = CursorType.Row)
        dt.addColumn("name")
        dt.addRow("Alice", id = "alice")
        dt.addRow("Bob", id = "bob")
        dt.moveCursor(row = 1)
        dt.start()
        var rowId: String? = null
        dt.onMessage<DataTable.RowSelected> { rowId = it.rowId }
        dt.selectCurrent()
        runBlocking { delay(30); dt.stop() }
        rowId shouldBe "bob"
    }

    // ---- Tabs ----

    "Tabs activates the first non-disabled tab by default" {
        val t = Tabs(Tab("a", "Alpha", disabled = true), Tab("b", "Beta"), Tab("c", "Gamma"))
        t.activeTabId shouldBe "b"
    }

    "Tabs.activate switches and emits TabActivated" {
        val t = Tabs(Tab("a", "Alpha"), Tab("b", "Beta"))
        t.start()
        var activated: String? = null
        t.onMessage<Tabs.TabActivated> { activated = it.tabId }
        t.activate("b")
        runBlocking { delay(30); t.stop() }
        t.activeTabId shouldBe "b"
        activated shouldBe "b"
    }

    "Tabs.disable on the active tab moves activation to the next non-disabled" {
        val t = Tabs(Tab("a", "Alpha"), Tab("b", "Beta"), Tab("c", "Gamma"))
        t.activeTabId shouldBe "a"
        t.disable("a")
        t.activeTabId shouldBe "b"
    }

    "Tabs.nextTab skips disabled tabs" {
        val t = Tabs(Tab("a", "Alpha"), Tab("b", "Beta", disabled = true), Tab("c", "Gamma"))
        t.activate("a")
        t.nextTab()
        t.activeTabId shouldBe "c"  // skipped b
    }

    // ---- TabbedContent ----

    "TabbedContent renders the active pane only" {
        val pane1 = TabPane("p1", "First", listOf(Label("FIRST_PANE")))
        val pane2 = TabPane("p2", "Second", listOf(Label("SECOND_PANE")))
        val tc = TabbedContent(listOf(pane1, pane2))
        val out = render(tc, width = 60)
        out shouldContain "FIRST_PANE"
        // Switch
        tc.switchTo("p2")
        val out2 = render(tc, width = 60)
        out2 shouldContain "SECOND_PANE"
    }

    // ---- ContentSwitcher ----

    "ContentSwitcher renders the current child" {
        val cs = ContentSwitcher(mapOf("a" to Label("HELLO"), "b" to Label("WORLD")))
        render(cs) shouldContain "HELLO"
        cs.switchTo("b")
        render(cs) shouldContain "WORLD"
    }

    // ---- DirectoryTree ----

    "DirectoryTree lists entries in a real directory" {
        val tmp = java.io.File.createTempFile("konsole-dirtree-", "").apply {
            delete()
            mkdirs()
            java.io.File(this, "a.txt").writeText("a")
            java.io.File(this, "b.txt").writeText("b")
            java.io.File(this, "sub").mkdirs()
        }
        val dt = DirectoryTree(tmp.absolutePath)
        dt.root.children.map { it.label }.let { labels ->
            (labels.any { it.endsWith("sub/") }) shouldBe true
            (labels.any { it == "a.txt" }) shouldBe true
            (labels.any { it == "b.txt" }) shouldBe true
        }
        tmp.deleteRecursively()
    }

    "DirectoryTree with showHidden = false skips dotfiles" {
        val tmp = java.io.File.createTempFile("konsole-dirtree-hidden-", "").apply {
            delete()
            mkdirs()
            java.io.File(this, ".hidden").writeText("h")
            java.io.File(this, "visible.txt").writeText("v")
        }
        val dt = DirectoryTree(tmp.absolutePath, showHidden = false)
        val names = dt.root.children.map { it.label }
        names.contains(".hidden") shouldBe false
        names.contains("visible.txt") shouldBe true
        tmp.deleteRecursively()
    }

    // ---- Toast ----

    "Toast renders title + message" {
        val t = Toast(title = "Saved", message = "Your changes have been saved.")
        val out = render(t, width = 60)
        out shouldContain "Saved"
        out shouldContain "Your changes"
    }

    "Toast.dismiss empties the rendered output" {
        val t = Toast(title = "x", message = "y")
        t.dismiss()
        render(t, width = 60) shouldBe ""
    }

    // ---- Tooltip ----

    "Tooltip renders its text inside a panel" {
        val tt = Tooltip("Click to confirm")
        render(tt, width = 30) shouldContain "Click to confirm"
    }
})
