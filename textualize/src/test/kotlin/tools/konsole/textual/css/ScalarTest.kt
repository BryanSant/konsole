package tools.konsole.textual.css

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ScalarTest : StringSpec({

    "parse cells, percent, fr, auto" {
        Scalar.parse("40")?.unit shouldBe Unit.Cells
        Scalar.parse("50%")?.unit shouldBe Unit.Percent
        Scalar.parse("1fr")?.unit shouldBe Unit.Fraction
        Scalar.parse("auto")?.unit shouldBe Unit.Auto
    }

    "resolve cells is identity" {
        Scalar(40.0, Unit.Cells).resolve(100) shouldBe 40
    }

    "resolve percent scales to container" {
        Scalar(50.0, Unit.Percent).resolve(80) shouldBe 40
    }

    "resolve fraction divides by total fractions" {
        Scalar(1.0, Unit.Fraction).resolve(60, totalFractions = 3.0) shouldBe 20
        Scalar(2.0, Unit.Fraction).resolve(60, totalFractions = 3.0) shouldBe 40
    }

    "auto resolves to sentinel -1 for layout to handle" {
        Scalar.AUTO.resolve(100) shouldBe -1
    }

    "parse rejects garbage" {
        Scalar.parse("xyz") shouldBe null
        Scalar.parse("") shouldBe null
    }
})
