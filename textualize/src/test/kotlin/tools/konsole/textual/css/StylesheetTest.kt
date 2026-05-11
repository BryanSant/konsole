package tools.konsole.textual.css

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import tools.konsole.core.style.Color
import tools.konsole.textual.widget.Widget

private class Header(id: String? = null, classes: Set<String> = emptySet()) : Widget(id, classes)
private class Button(id: String? = null, classes: Set<String> = emptySet()) : Widget(id, classes)

class StylesheetTest : StringSpec({

    "parse and apply a single rule with type selector" {
        val sheet = Stylesheet.parse("Header { color: red; }")
        val s = sheet.apply(Header())
        s.color shouldBe Color.Red
    }

    "id selector overrides class selector by specificity" {
        val sheet = Stylesheet.parse("""
            .warn { color: yellow; }
            #main { color: blue; }
        """.trimIndent())
        val widget = Header(id = "main", classes = setOf("warn"))
        sheet.apply(widget).color shouldBe Color.Blue
    }

    "class selector overrides type selector by specificity" {
        val sheet = Stylesheet.parse("""
            Header { color: red; }
            .warn { color: yellow; }
        """.trimIndent())
        val widget = Header(classes = setOf("warn"))
        sheet.apply(widget).color shouldBe Color.Yellow
    }

    "later declaration wins among same-specificity rules" {
        val sheet = Stylesheet.parse("""
            Header { color: red; }
            Header { color: green; }
        """.trimIndent())
        sheet.apply(Header()).color shouldBe Color.Green
    }

    "padding shorthand: one value" {
        val sheet = Stylesheet.parse("Header { padding: 2; }")
        val s = sheet.apply(Header())
        s.padding shouldBe tools.konsole.rich.geometry.Spacing.all(2)
    }

    "width parses to Scalar" {
        val sheet = Stylesheet.parse("Header { width: 50%; }")
        val s = sheet.apply(Header())
        s.width?.unit shouldBe Unit.Percent
        s.width?.value shouldBe 50.0
    }

    "comma-separated selector list matches both" {
        val sheet = Stylesheet.parse("Header, Button { color: red; }")
        sheet.apply(Header()).color shouldBe Color.Red
        sheet.apply(Button()).color shouldBe Color.Red
    }

    "unknown properties are ignored" {
        val sheet = Stylesheet.parse("Header { foo-bar: baz; color: red; }")
        sheet.apply(Header()).color shouldBe Color.Red
    }
})
