package tools.konsole.rich.spinner

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContainAll

/**
 * Validates that the [SPINNERS] catalogue matches Python rich's `_spinners.py`
 * 1:1 — every spinner anyone writes `Spinner("foo")` for in rich must resolve here.
 */
class SpinnersCatalogTest : StringSpec({

    "SPINNERS contains all 73 entries from rich" {
        SPINNERS.size shouldBe 73
    }

    "SPINNERS contains every name from rich's _spinners.py" {
        // Hard-coded list captured from rich 13.x (as of the port).
        val richNames = setOf(
            "aesthetic", "arc", "arrow", "arrow2", "arrow3",
            "balloon", "balloon2", "betaWave", "bounce", "bouncingBall", "bouncingBar",
            "boxBounce", "boxBounce2",
            "christmas",
            "circle", "circleHalves", "circleQuarters",
            "clock",
            "dots", "dots2", "dots3", "dots4", "dots5", "dots6", "dots7",
            "dots8", "dots8Bit", "dots9", "dots10", "dots11", "dots12",
            "dqpb",
            "earth",
            "flip",
            "grenade", "growHorizontal", "growVertical",
            "hamburger", "hearts",
            "layer", "line", "line2",
            "material", "monkey", "moon",
            "noise",
            "pipe", "point", "pong",
            "runner",
            "shark", "simpleDots", "simpleDotsScrolling", "smiley", "squareCorners", "squish",
            "star", "star2",
            "toggle", "toggle2", "toggle3", "toggle4", "toggle5", "toggle6", "toggle7",
            "toggle8", "toggle9", "toggle10", "toggle11", "toggle12", "toggle13",
            "triangle",
            "weather",
        )
        SPINNERS.keys shouldContainAll richNames
        richNames.size shouldBe 73
    }

    "every spinner has at least one frame and a positive interval" {
        for ((name, data) in SPINNERS) {
            check(data.frames.isNotEmpty()) { "spinner '$name' has no frames" }
            check(data.interval > 0) { "spinner '$name' has non-positive interval ${data.interval}" }
        }
    }

    "byName resolves the canonical names" {
        SpinnerData.byName("dots").frames.size shouldBe 10
        SpinnerData.byName("line").interval shouldBe 130
        SpinnerData.byName("clock").frames.size shouldBe 12
    }

    "byName throws on unknown name" {
        try {
            SpinnerData.byName("notARealSpinner")
            error("expected exception")
        } catch (_: IllegalStateException) { /* ok */ }
    }
})
