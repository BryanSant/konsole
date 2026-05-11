package tools.konsole.core.command

import tools.konsole.core.Command
import tools.konsole.core.Hide
import tools.konsole.core.MoveDown
import tools.konsole.core.MoveTo
import tools.konsole.core.MoveToColumn
import tools.konsole.core.MoveToRow
import tools.konsole.core.RestorePosition
import tools.konsole.core.SavePosition
import tools.konsole.core.Show
import tools.konsole.core.queue
import tools.konsole.core.style.Print
import tools.konsole.core.terminal.Clear
import tools.konsole.core.terminal.ClearType
import tools.konsole.core.terminal.EnterAlternateScreen
import tools.konsole.core.terminal.LeaveAlternateScreen
import tools.konsole.core.terminal.SetSize
import tools.konsole.core.terminal.SetTitle
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private fun ansi(c: Command): String = StringBuilder().also(c::writeAnsi).toString()

class SerializationSpec : StringSpec({

    // ANSI control characters are spelled in test fixtures as Unicode escapes
    // for clarity; the runtime string equality checks the actual byte sequences.
    val esc = "\u001B"
    val csi = "$esc["

    "MoveTo emits 1-indexed CSI row;col H" {
        ansi(MoveTo(0, 0)) shouldBe "${csi}1;1H"
        ansi(MoveTo(4, 7)) shouldBe "${csi}8;5H"
    }

    "MoveToColumn / MoveToRow emit 1-indexed values" {
        ansi(MoveToColumn(0)) shouldBe "${csi}1G"
        ansi(MoveToRow(9))    shouldBe "${csi}10d"
    }

    "MoveDown(0) emits nothing" {
        ansi(MoveDown(0)) shouldBe ""
    }

    "Hide / Show emit DECTCEM" {
        ansi(Hide) shouldBe "${csi}?25l"
        ansi(Show) shouldBe "${csi}?25h"
    }

    "SavePosition / RestorePosition emit DECSC / DECRC" {
        ansi(SavePosition)    shouldBe "${esc}7"
        ansi(RestorePosition) shouldBe "${esc}8"
    }

    "Clear emits the right N J / N K code per type" {
        ansi(Clear(ClearType.All))             shouldBe "${csi}2J"
        ansi(Clear(ClearType.Purge))           shouldBe "${csi}3J"
        ansi(Clear(ClearType.FromCursorDown))  shouldBe "${csi}J"
        ansi(Clear(ClearType.FromCursorUp))    shouldBe "${csi}1J"
        ansi(Clear(ClearType.CurrentLine))     shouldBe "${csi}2K"
        ansi(Clear(ClearType.UntilNewLine))    shouldBe "${csi}K"
    }

    "EnterAlternateScreen / LeaveAlternateScreen toggle DEC mode 1049" {
        ansi(EnterAlternateScreen) shouldBe "${csi}?1049h"
        ansi(LeaveAlternateScreen) shouldBe "${csi}?1049l"
    }

    "SetSize emits CSI 8;rows;cols t" {
        ansi(SetSize(80, 24)) shouldBe "${csi}8;24;80t"
    }

    "SetTitle emits OSC 0;<title>ST" {
        ansi(SetTitle("hello")) shouldBe "${esc}]0;hello${esc}\\"
    }

    "Print emits the toString verbatim" {
        ansi(Print("hello"))   shouldBe "hello"
        ansi(Print(42))        shouldBe "42"
        ansi(Print(null))      shouldBe "null"
    }

    "queue concatenates commands" {
        val out = StringBuilder()
        out.queue(MoveTo(0, 0), Hide, Print("hi"))
        out.toString() shouldBe "${csi}1;1H${csi}?25lhi"
    }

    "Custom Command via SAM works" {
        val custom = Command { it.append("X") }
        ansi(custom) shouldBe "X"
    }
})
