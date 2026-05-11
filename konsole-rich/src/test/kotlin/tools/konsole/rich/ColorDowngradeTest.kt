package tools.konsole.rich

import tools.konsole.core.ColorSystem

import tools.konsole.core.style.Color
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ColorDowngradeTest : StringSpec({

    "rgb pure red downgrades to ansi 196" {
        // 6×6×6 cube: r=5, g=0, b=0 → 16 + 36*5 + 6*0 + 0 = 196
        Color.Rgb(255, 0, 0).toAnsi256() shouldBe Color.AnsiValue(196)
    }

    "rgb pure white downgrades to ansi 231" {
        Color.Rgb(255, 255, 255).toAnsi256() shouldBe Color.AnsiValue(231)
    }

    "rgb pure black downgrades to ansi 16" {
        Color.Rgb(0, 0, 0).toAnsi256() shouldBe Color.AnsiValue(16)
    }

    "rgb gray uses grayscale ramp" {
        // gray ~128 should land around index 244: 232 + (128-8)/10 = 232 + 12 = 244
        Color.Rgb(128, 128, 128).toAnsi256() shouldBe Color.AnsiValue(244)
    }

    "rgb red downgrades to named16 Red" {
        Color.Rgb(255, 0, 0).toNamed16() shouldBe Color.Red
    }

    "rgb dark red downgrades to named16 DarkRed" {
        Color.Rgb(128, 0, 0).toNamed16() shouldBe Color.DarkRed
    }

    "ansi value 9 returns named Red directly" {
        Color.AnsiValue(9).toNamed16() shouldBe Color.Red
    }

    "ColorSystem.None downgrades any color to Reset" {
        ColorSystem.None.downgrade(Color.Rgb(255, 0, 0)) shouldBe Color.Reset
        ColorSystem.None.downgrade(Color.Red) shouldBe Color.Reset
    }

    "ColorSystem.TrueColor preserves rgb" {
        val rgb = Color.Rgb(123, 45, 67)
        ColorSystem.TrueColor.downgrade(rgb) shouldBe rgb
    }

    "ColorSystem.EightBit downgrades RGB" {
        ColorSystem.EightBit.downgrade(Color.Rgb(255, 0, 0)) shouldBe Color.AnsiValue(196)
        ColorSystem.EightBit.downgrade(Color.AnsiValue(123)) shouldBe Color.AnsiValue(123) // unchanged
    }

    "ColorSystem.Standard downgrades to named16" {
        ColorSystem.Standard.downgrade(Color.Rgb(255, 0, 0)) shouldBe Color.Red
    }
})
