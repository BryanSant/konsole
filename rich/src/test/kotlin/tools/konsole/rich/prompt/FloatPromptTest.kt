package tools.konsole.rich.prompt

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private class TestableFloatPrompt : FloatPrompt(
    prompt = "x",
    console = tools.konsole.rich.Console.string(),
) {
    fun parse(value: String): Double = process(value)
}

class FloatPromptTest : StringSpec({

    "process parses doubles" {
        val p = TestableFloatPrompt()
        p.parse("3.14") shouldBe 3.14
        p.parse("-0.5") shouldBe -0.5
        p.parse("1e3") shouldBe 1000.0
    }

    "process throws InvalidResponse on non-numeric input" {
        val p = TestableFloatPrompt()
        try {
            p.parse("not-a-number")
            error("expected InvalidResponse")
        } catch (_: InvalidResponse) { /* ok */ }
    }
})
