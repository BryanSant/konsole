package tools.konsole.core.progress

import tools.konsole.core.ClearProgress
import tools.konsole.core.Command
import tools.konsole.core.ProgressState
import tools.konsole.core.SetProgress
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith

private fun ansi(c: Command): String = StringBuilder().also(c::writeAnsi).toString()

class ProgressSpec : StringSpec({
    val osc = "\u001B]"
    val st  = "\u001B\\"

    "Remove emits state 0" {
        ansi(SetProgress(ProgressState.Remove)) shouldBe "${osc}9;4;0;0${st}"
    }

    "Normal emits state 1 with percent" {
        ansi(SetProgress(ProgressState.Normal(50))) shouldBe "${osc}9;4;1;50${st}"
    }

    "Error emits state 2 with percent" {
        ansi(SetProgress(ProgressState.Error(75))) shouldBe "${osc}9;4;2;75${st}"
    }

    "Indeterminate emits state 3 with zero percent" {
        ansi(SetProgress(ProgressState.Indeterminate)) shouldBe "${osc}9;4;3;0${st}"
    }

    "Paused emits state 4 with percent" {
        ansi(SetProgress(ProgressState.Paused(30))) shouldBe "${osc}9;4;4;30${st}"
    }

    "ClearProgress is equivalent to Remove" {
        ansi(ClearProgress) shouldBe ansi(SetProgress(ProgressState.Remove))
    }

    "Normal rejects percent out of range" {
        shouldThrow<IllegalArgumentException> { SetProgress(ProgressState.Normal(101)) }
        shouldThrow<IllegalArgumentException> { SetProgress(ProgressState.Normal(-1)) }
    }

    "Error rejects percent out of range" {
        shouldThrow<IllegalArgumentException> { SetProgress(ProgressState.Error(101)) }
    }

    "Paused rejects percent out of range" {
        shouldThrow<IllegalArgumentException> { SetProgress(ProgressState.Paused(-1)) }
    }

    "boundary values 0 and 100 are accepted" {
        ansi(SetProgress(ProgressState.Normal(0)))   shouldEndWith "1;0${st}"
        ansi(SetProgress(ProgressState.Normal(100))) shouldEndWith "1;100${st}"
    }
})
