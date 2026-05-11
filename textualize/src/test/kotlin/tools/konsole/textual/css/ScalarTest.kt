package tools.konsole.textual.css

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ScalarTest : StringSpec({

    "parse cells, percent, fr, auto" {
        Scalar.parse("40")?.unit shouldBe LengthUnit.Cells
        Scalar.parse("50%")?.unit shouldBe LengthUnit.Percent
        Scalar.parse("1fr")?.unit shouldBe LengthUnit.Fraction
        Scalar.parse("auto")?.unit shouldBe LengthUnit.Auto
    }

    "resolve cells is identity" {
        Scalar(40.0, LengthUnit.Cells).resolve(100) shouldBe 40
    }

    "resolve percent scales to container" {
        Scalar(50.0, LengthUnit.Percent).resolve(80) shouldBe 40
    }

    "resolve fraction divides by total fractions" {
        Scalar(1.0, LengthUnit.Fraction).resolve(60, totalFractions = 3.0) shouldBe 20
        Scalar(2.0, LengthUnit.Fraction).resolve(60, totalFractions = 3.0) shouldBe 40
    }

    "auto resolves to sentinel -1 for layout to handle" {
        Scalar.AUTO.resolve(100) shouldBe -1
    }

    "parse rejects garbage" {
        Scalar.parse("xyz") shouldBe null
        Scalar.parse("") shouldBe null
    }
})
