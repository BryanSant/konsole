package tools.konsole.textual.widgets

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class CommandPaletteTest : StringSpec({

    "filter matches title substrings case-insensitively" {
        val cmds = listOf(
            Command("open", "Open File"),
            Command("save", "Save"),
            Command("quit", "Quit"),
        )
        val cp = CommandPalette(cmds)
        cp.filtered.size shouldBe 3
        // Simulate typing "ave" → only "Save" matches.
        cp.input.start()
        cp.input.insert("ave")
        runBlocking { delay(40) }
        cp.filtered.size shouldBe 1
        cp.filtered.single().id shouldBe "save"
    }

    "filter matches keywords" {
        val cmds = listOf(
            Command("close", "Close File", keywords = listOf("exit", "shut")),
            Command("save", "Save"),
        )
        val cp = CommandPalette(cmds)
        cp.input.start()
        cp.input.insert("exit")
        runBlocking { delay(40) }
        cp.filtered.single().id shouldBe "close"
    }

    "blank query restores the full list" {
        val cmds = listOf(Command("a", "Alpha"), Command("b", "Beta"))
        val cp = CommandPalette(cmds)
        cp.input.start()
        cp.input.insert("alp")
        runBlocking { delay(40) }
        cp.filtered.size shouldBe 1
        cp.input.clear()
        runBlocking { delay(40) }
        cp.filtered.size shouldBe 2
    }
})
