package tools.konsole.core.style

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ColorParseSpec : StringSpec({

    // ── named colors ──────────────────────────────────────────────────────────

    "parse named colors" {
        Color.parse("red")        shouldBe Color.Red
        Color.parse("dark_blue")  shouldBe Color.DarkBlue
        Color.parse("grey")       shouldBe Color.Grey
        Color.parse("gray")       shouldBe Color.Grey
        Color.parse("dark_gray")  shouldBe Color.DarkGrey
        Color.parse("reset")      shouldBe Color.Reset
    }

    "parse is case-insensitive and trims whitespace" {
        Color.parse("  RED  ")    shouldBe Color.Red
        Color.parse("DARK_CYAN")  shouldBe Color.DarkCyan
    }

    // ── rgb_() format ─────────────────────────────────────────────────────────

    "parse rgb() format" {
        Color.parse("rgb(255,128,0)")  shouldBe Color.Rgb(255, 128, 0)
        Color.parse("rgb(0, 0, 0)")    shouldBe Color.Rgb(0, 0, 0)
    }

    // ── ansi() format ─────────────────────────────────────────────────────────

    "parse ansi() format" {
        Color.parse("ansi(0)")   shouldBe Color.AnsiValue(0)
        Color.parse("ansi(196)") shouldBe Color.AnsiValue(196)
        Color.parse("ansi(255)") shouldBe Color.AnsiValue(255)
    }

    // ── hex #rrggbb ───────────────────────────────────────────────────────────

    "parse 6-digit hex lowercase" {
        Color.parse("#a7b8c9") shouldBe Color.Rgb(0xa7, 0xb8, 0xc9)
    }

    "parse 6-digit hex uppercase (case-folded)" {
        Color.parse("#A7B8C9") shouldBe Color.Rgb(0xa7, 0xb8, 0xc9)
    }

    "parse 6-digit hex black and white" {
        Color.parse("#000000") shouldBe Color.Rgb(0, 0, 0)
        Color.parse("#ffffff") shouldBe Color.Rgb(255, 255, 255)
    }

    "parse 6-digit hex primary colors" {
        Color.parse("#ff0000") shouldBe Color.Rgb(255, 0, 0)
        Color.parse("#00ff00") shouldBe Color.Rgb(0, 255, 0)
        Color.parse("#0000ff") shouldBe Color.Rgb(0, 0, 255)
    }

    // ── hex #rgb ──────────────────────────────────────────────────────────────

    "parse 3-digit hex expands each nibble" {
        Color.parse("#fff") shouldBe Color.Rgb(255, 255, 255)
        Color.parse("#000") shouldBe Color.Rgb(0, 0, 0)
        Color.parse("#f00") shouldBe Color.Rgb(255, 0, 0)
        Color.parse("#0f0") shouldBe Color.Rgb(0, 255, 0)
        Color.parse("#00f") shouldBe Color.Rgb(0, 0, 255)
    }

    "parse 3-digit hex mid-range value" {
        // #a7c → #aa77cc
        Color.parse("#a7c") shouldBe Color.Rgb(0xaa, 0x77, 0xcc)
    }

    "parse 3-digit hex uppercase (case-folded)" {
        Color.parse("#FFF") shouldBe Color.Rgb(255, 255, 255)
        Color.parse("#F0A") shouldBe Color.Rgb(255, 0, 170)
    }

    // ── invalid inputs return null ────────────────────────────────────────────

    "unknown name returns null" {
        Color.parse("turquoise") shouldBe null
        Color.parse("")          shouldBe null
    }

    "hex with wrong length returns null" {
        Color.parse("#12")       shouldBe null   // too short
        Color.parse("#1234")     shouldBe null   // 4 digits — not a valid form
        Color.parse("#12345")    shouldBe null   // 5 digits
        Color.parse("#1234567")  shouldBe null   // 7 digits — too long
    }

    "hex with invalid characters returns null" {
        Color.parse("#zzzzzz") shouldBe null
        Color.parse("#gg0")    shouldBe null
    }

    "hex without leading # returns null" {
        Color.parse("ff0000") shouldBe null
    }
})
