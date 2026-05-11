package tools.konsole.textual.layouts

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import tools.konsole.rich.Renderable
import tools.konsole.rich.Text
import tools.konsole.rich.geometry.Region
import tools.konsole.textual.css.LengthUnit
import tools.konsole.textual.css.Scalar
import tools.konsole.textual.css.Styles
import tools.konsole.textual.widget.Widget

/** Test widget whose `render()` returns text of a known width. */
private class FixedText(private val text: String) : Widget() {
    override fun render(): Renderable = Text(text)
}

class GridAutoTrackTest : StringSpec({

    "auto column shrinks to fit its widest child" {
        // 2 columns: auto + 1fr. The auto track should take the width of the
        // longest child in that column ("widest" is 12 chars), and the 1fr
        // track absorbs the remaining 18 of a 30-wide region.
        val short = FixedText("hi")
        val mid   = FixedText("widest label")
        val rest  = FixedText("x")
        val rest2 = FixedText("y")
        val children = listOf(
            short to Styles.NULL,
            rest to Styles.NULL,
            mid to Styles.NULL,
            rest2 to Styles.NULL,
        )
        val placements = GridLayout.arrangeWithParent(
            region = Region(0, 0, 30, 4),
            children = children,
            parentStyles = Styles(
                gridSize = 2 to 2,
                gridColumns = listOf(
                    Scalar(0.0, LengthUnit.Auto),
                    Scalar(1.0, LengthUnit.Fraction),
                ),
            ),
        )
        // Column 0's width is the widest child's natural width (12 cells).
        placements[0].region.width shouldBe 12  // "hi" in col 0
        placements[1].region.width shouldBe 18  // "x" in col 1 = 30 - 12
        placements[2].region.x shouldBe 0
        placements[2].region.width shouldBe 12  // "widest label" in col 0
    }

    "auto row shrinks to fit its tallest child" {
        // 1 column, 2 rows: auto + 1fr. Row 0 holds a one-line widget; row 1
        // holds the rest. Row 0 should be 1 cell tall.
        val oneline = FixedText("only one line")
        val anything = FixedText("anything")
        val placements = GridLayout.arrangeWithParent(
            region = Region(0, 0, 20, 10),
            children = listOf(oneline to Styles.NULL, anything to Styles.NULL),
            parentStyles = Styles(
                gridSize = 1 to 2,
                gridRows = listOf(
                    Scalar(0.0, LengthUnit.Auto),
                    Scalar(1.0, LengthUnit.Fraction),
                ),
            ),
        )
        placements[0].region.height shouldBe 1   // measured
        placements[1].region.height shouldBe 9   // remainder
    }

    "mix of fixed + auto + fr distributes correctly" {
        // 3 columns: 10cells, auto, 1fr. Auto should fit "hello" (5 chars).
        // 1fr gets 40 - 10 - 5 = 25.
        val a = FixedText("a")
        val b = FixedText("hello")
        val c = FixedText("c")
        val placements = GridLayout.arrangeWithParent(
            region = Region(0, 0, 40, 4),
            children = listOf(a to Styles.NULL, b to Styles.NULL, c to Styles.NULL),
            parentStyles = Styles(
                gridSize = 3 to 1,
                gridColumns = listOf(
                    Scalar(10.0, LengthUnit.Cells),
                    Scalar(0.0, LengthUnit.Auto),
                    Scalar(1.0, LengthUnit.Fraction),
                ),
            ),
        )
        placements[0].region.width shouldBe 10
        placements[1].region.width shouldBe 5
        placements[2].region.width shouldBe 25
    }
})
