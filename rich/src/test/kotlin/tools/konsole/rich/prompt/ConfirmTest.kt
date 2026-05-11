package tools.konsole.rich.prompt

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * We don't drive an actual stdin in tests — these target the parsing/validation in
 * [Confirm.process] via reflection so we can verify the contract.
 */
class ConfirmTest : StringSpec({

    "process accepts y / yes / true / 1" {
        val confirm = Confirm("?", tools.konsole.rich.Console.string())
        val process = confirm::class.java.superclass.getDeclaredMethod("process", String::class.java).apply { isAccessible = true }
        for (yes in listOf("y", "Y", "yes", "true", "1")) {
            (process.invoke(confirm, yes) as Boolean) shouldBe true
        }
        for (no in listOf("n", "N", "no", "false", "0")) {
            (process.invoke(confirm, no) as Boolean) shouldBe false
        }
    }

    "process throws on invalid input" {
        val confirm = Confirm("?", tools.konsole.rich.Console.string())
        val process = confirm::class.java.superclass.getDeclaredMethod("process", String::class.java).apply { isAccessible = true }
        try {
            process.invoke(confirm, "maybe")
            error("expected InvalidResponse")
        } catch (e: java.lang.reflect.InvocationTargetException) {
            (e.targetException is InvalidResponse) shouldBe true
        }
    }
})
