package tools.konsole.core.style

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith

class StylizeSpec : StringSpec({

    val esc = '\u001B'

    "red() renders SGR 38;5;9" {
        "hi".red().toString() shouldBe "$esc[38;5;9mhi$esc[0m"
    }

    "bold() renders SGR 1" {
        "hi".bold().toString() shouldBe "$esc[1mhi$esc[0m"
    }

    "chained red().bold() merges into one SGR" {
        val s = "warn".red().bold().toString()
        s shouldStartWith "$esc["
        s shouldContain "38;5;9"
        s shouldContain ";1m"            // bold lands in the same SGR as fg color
        s shouldContain "warn"
        s shouldEndWith "$esc[0m"
    }

    "onBlue() sets background SGR 48;5;12" {
        "hi".onBlue().toString() shouldContain "48;5;12"
    }

    "with(Rgb) renders 38;2;r;g;b" {
        "hi".with(Color.Rgb(10, 20, 30)).toString() shouldContain "38;2;10;20;30"
    }

    "AnsiValue palette renders 38;5;n" {
        "hi".with(Color.AnsiValue(196)).toString() shouldContain "38;5;196"
    }

    "later color overwrites earlier" {
        val s = "hi".red().green().toString()
        s shouldContain "38;5;10"   // green
        // red shouldn't appear (since with() replaces foreground)
        (s.contains("38;5;9;") || s.contains(";38;5;9")) shouldBe false
    }

    "attributes accumulate" {
        val s = "hi".bold().italic().underlined().toString()
        s shouldContain "1"
        s shouldContain "3"
        s shouldContain "4"
    }
})
