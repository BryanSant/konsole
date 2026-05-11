package tools.konsole.textual.css

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File

class StylesheetWatcherTest : StringSpec({

    "watcher fires onChange when the watched file is modified" {
        val tmp = File.createTempFile("konsole-tcss-", ".tcss")
        tmp.writeText("Widget { color: red; }")
        try {
            var fired = 0
            var lastContent = ""
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val watcher = StylesheetWatcher(
                file = tmp,
                scope = scope,
                onChange = { content ->
                    fired += 1
                    lastContent = content
                    kotlin.Unit
                },
                debounceMs = 50L,
            )
            watcher.start()
            runBlocking {
                delay(150)
                tmp.writeText("Widget { color: green; }")
                delay(500)
            }
            scope.cancel()
            (fired >= 1) shouldBe true
            lastContent.contains("green") shouldBe true
        } finally {
            tmp.delete()
        }
    }

    "stylesheet hot-reload via App.watchStylesheet" {
        val tmp = File.createTempFile("konsole-app-tcss-", ".tcss")
        tmp.writeText("Widget { color: red; }")
        try {
            val app = object : tools.konsole.textual.app.App(
                tools.konsole.textual.driver.HeadlessDriver()
            ) {}
            app.start()
            app.watchStylesheet(tmp)
            // Initial load should produce a stylesheet with one rule.
            app.stylesheet.rules.size shouldBe 1
            runBlocking {
                delay(150)
                tmp.writeText("""
                    Widget { color: red; }
                    Button { color: blue; }
                    Label { color: green; }
                """.trimIndent())
                delay(600)
            }
            // After hot-reload, three rules.
            (app.stylesheet.rules.size >= 3) shouldBe true
            app.stopWatchingStylesheet()
            app.stop()
        } finally {
            tmp.delete()
        }
    }
})
