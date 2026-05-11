package tools.konsole.textual.css

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import tools.konsole.core.style.Color
import tools.konsole.textual.widget.Widget

private class TestWidget(
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes)

class PseudoClassMatchTest : StringSpec({

    "selector with :hover matches only when widget is hovered" {
        val sheet = Stylesheet.parse("Button:hover { color: red; }")
        val w = TestWidget()
        // hasFocus/isHovered are internal-set; tests use reflection-free state setters via the
        // package-internal field. Here we exercise the matcher directly with a TestWidget whose
        // pseudo set we toggle.
        val noHover = sheet.apply(w)
        noHover.color shouldBe null

        // Build a node with hover active by overriding activePseudoClasses:
        val hovering = object : Widget() {
            override val cssType: String = "Button"
            override val activePseudoClasses: Set<String> get() = setOf("hover", "enabled")
        }
        val withHover = sheet.apply(hovering)
        withHover.color shouldBe Color.Red
    }

    "selector with :focus matches the focused widget" {
        val sheet = Stylesheet.parse("Input:focus { color: cyan; }")
        val unfocused = object : Widget() {
            override val cssType: String = "Input"
        }
        val focused = object : Widget() {
            override val cssType: String = "Input"
            override val activePseudoClasses: Set<String> get() = setOf("focus", "enabled")
        }
        sheet.apply(unfocused).color shouldBe null
        sheet.apply(focused).color shouldBe Color.Cyan
    }

    "selector with :disabled matches a disabled widget and :enabled matches its complement" {
        val sheet = Stylesheet.parse("""
            Button:disabled { color: red; }
            Button:enabled  { color: green; }
        """.trimIndent())
        val enabled = object : Widget() {
            override val cssType: String = "Button"
            override val activePseudoClasses: Set<String> get() = setOf("enabled")
        }
        val disabled = object : Widget() {
            override val cssType: String = "Button"
            override val activePseudoClasses: Set<String> get() = setOf("disabled")
        }
        sheet.apply(enabled).color shouldBe Color.Green
        sheet.apply(disabled).color shouldBe Color.Red
    }

    "specificity: base + :hover overlay correctly" {
        val sheet = Stylesheet.parse("""
            Button         { color: white; background: black; }
            Button:hover   { background: blue; }
        """.trimIndent())
        val idle = object : Widget() { override val cssType: String = "Button" }
        val hovered = object : Widget() {
            override val cssType: String = "Button"
            override val activePseudoClasses: Set<String> get() = setOf("hover", "enabled")
        }
        // Idle: just the base rule.
        sheet.apply(idle).color shouldBe Color.White
        sheet.apply(idle).background shouldBe Color.Black
        // Hovered: hover rule wins for background (higher specificity), color stays.
        sheet.apply(hovered).color shouldBe Color.White
        sheet.apply(hovered).background shouldBe Color.Blue
    }
})
