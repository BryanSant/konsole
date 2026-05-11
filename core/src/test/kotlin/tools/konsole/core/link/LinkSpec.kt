package tools.konsole.core.link

import tools.konsole.core.Ansi
import tools.konsole.core.Command
import tools.konsole.core.EndLink
import tools.konsole.core.StartLink
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private fun ansi(c: Command): String = StringBuilder().also(c::writeAnsi).toString()

class LinkSpec : StringSpec({

    val esc = Ansi.ESC

    "StartLink with no params emits OSC 8;;<uri>ST" {
        ansi(StartLink("https://example.com")) shouldBe "${esc}]8;;https://example.com${esc}\\"
    }

    "StartLink with params emits k=v separated by ':'" {
        val s = ansi(StartLink("https://example.com", mapOf("id" to "42")))
        s shouldBe "${esc}]8;id=42;https://example.com${esc}\\"
    }

    "EndLink emits OSC 8;;ST" {
        ansi(EndLink) shouldBe "${esc}]8;;${esc}\\"
    }
})
