package tools.konsole.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Pure logic test of [ColorSystem.detect]. Because [ColorSystem.detect] reads
 * from `System.getenv()`, we cover the env-var matrix indirectly: each case
 * sets the relevant variables for the duration of the test using a custom
 * environment shim isn't trivially possible on the JVM. Instead we structure
 * the tests to read the *current* environment as the baseline and only
 * assert behavior that holds regardless of host environment.
 *
 * For full coverage of the matrix, see `ColorSystemMatrixTest` (Tty-tagged)
 * which spawns subprocesses with controlled env.
 */
class ColorSystemSpec : StringSpec({

    "detect returns a non-null value for both isTty=true and isTty=false" {
        ColorSystem.detect(isTty = true) shouldNotBe null
        ColorSystem.detect(isTty = false) shouldNotBe null
    }

    "ColorSystem enum is ordered None < Standard < EightBit < TrueColor" {
        ColorSystem.None.ordinal shouldBe 0
        ColorSystem.Standard.ordinal shouldBe 1
        ColorSystem.EightBit.ordinal shouldBe 2
        ColorSystem.TrueColor.ordinal shouldBe 3
    }

    "ColorSystem has Windows entry distinct from Standard" {
        ColorSystem.Windows.ordinal shouldBe 4
    }
})

private infix fun <T> T.shouldNotBe(other: T?) {
    if (this == other) error("expected $this to not equal $other")
}
