package tools.konsole.core.notification

import tools.konsole.core.Command
import tools.konsole.core.SendNotification
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private fun ansi(c: Command): String = StringBuilder().also(c::writeAnsi).toString()

class NotificationSpec : StringSpec({
    val osc = "\u001B]"
    val st  = "\u001B\\"

    "emits OSC 9 with body only" {
        ansi(SendNotification("title", "body")) shouldContain "${osc}9;body${st}"
    }

    "emits OSC 99 with title and body" {
        ansi(SendNotification("title", "body")) shouldContain "${osc}99;title;body${st}"
    }

    "emits OSC 777 with notify prefix, title, and body" {
        ansi(SendNotification("title", "body")) shouldContain "${osc}777;notify;title;body${st}"
    }

    "all three sequences are present in a single writeAnsi call" {
        val out = ansi(SendNotification("Build done", "Tests passed"))
        out shouldContain "${osc}9;Tests passed${st}"
        out shouldContain "${osc}99;Build done;Tests passed${st}"
        out shouldContain "${osc}777;notify;Build done;Tests passed${st}"
    }

    "empty title and body are accepted" {
        val out = ansi(SendNotification("", ""))
        out shouldContain "${osc}9;${st}"
        out shouldContain "${osc}99;;${st}"
        out shouldContain "${osc}777;notify;;${st}"
    }

    "data class equality" {
        SendNotification("t", "b") shouldBe SendNotification("t", "b")
    }
})
