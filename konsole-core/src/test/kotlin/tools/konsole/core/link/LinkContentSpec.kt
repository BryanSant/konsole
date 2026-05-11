package tools.konsole.core.link

import tools.konsole.core.LinkContent
import tools.konsole.core.link
import tools.konsole.core.style.bold
import tools.konsole.core.style.cyan
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith

class LinkContentSpec : StringSpec({

    val esc = "\u001B"
    val open  = { uri: String -> "${esc}]8;;$uri${esc}\\" }
    val close  = "${esc}]8;;${esc}\\"

    "String.link() wraps text in OSC 8 open and close" {
        "Click me".link("https://example.com").toString() shouldBe
            "${open("https://example.com")}Click me$close"
    }

    "String.link() with params encodes them before the URI" {
        "Click me".link("https://example.com", mapOf("id" to "42")).toString() shouldBe
            "${esc}]8;id=42;https://example.com${esc}\\Click me$close"
    }

    "StyledContent.link() places SGR codes inside the OSC 8 boundaries" {
        val result = "Click me".bold().cyan().link("https://example.com").toString()
        result shouldStartWith open("https://example.com")
        result shouldEndWith close
        result shouldContain "Click me"
        result shouldContain "${esc}["   // SGR open somewhere inside
        result shouldContain "${esc}[0m" // SGR reset before the OSC 8 close
    }

    "LinkContent.toString() is usable in a string template" {
        val lnk = "here".link("https://example.com")
        "Click $lnk now" shouldContain "here"
        "Click $lnk now" shouldContain open("https://example.com")
    }
})
