package tools.konsole.textual.layouts

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import tools.konsole.rich.geometry.Region
import tools.konsole.textual.css.LengthUnit
import tools.konsole.textual.css.Scalar
import tools.konsole.textual.css.Styles
import tools.konsole.textual.widget.Widget

private class Box(id: String? = null) : Widget(id, emptySet())

class GridLayoutTest : StringSpec({

    "4×4 grid evenly distributes a 40×20 region across 16 children" {
        val children = List(16) { Box() to Styles.NULL }
        val placements = GridLayout.arrangeWithParent(
            region = Region(0, 0, 40, 20),
            children = children,
            parentStyles = Styles(gridSize = 4 to 4),
        )
        placements.size shouldBe 16
        // Every cell should be 10 × 5 with no gutter.
        for (p in placements) {
            p.region.width shouldBe 10
            p.region.height shouldBe 5
        }
        // First cell at origin, last cell at (30, 15).
        placements.first().region.x shouldBe 0
        placements.first().region.y shouldBe 0
        placements.last().region.x shouldBe 30
        placements.last().region.y shouldBe 15
    }

    "grid gutter subtracts from cell sizes" {
        val children = List(4) { Box() to Styles.NULL }
        val placements = GridLayout.arrangeWithParent(
            region = Region(0, 0, 22, 22),
            children = children,
            parentStyles = Styles(gridSize = 2 to 2, gridGutter = 2 to 2),
        )
        // Two columns sharing 22 - 2 (gutter) = 20 cells → 10 wide each.
        placements[0].region.width shouldBe 10
        placements[1].region.x shouldBe 12   // 10 cells + 2 gutter
    }

    "explicit grid-columns mixes fixed and fr tracks" {
        val children = List(3) { Box() to Styles.NULL }
        val placements = GridLayout.arrangeWithParent(
            region = Region(0, 0, 30, 5),
            children = children,
            parentStyles = Styles(
                gridSize = 3 to 1,
                gridColumns = listOf(
                    Scalar(10.0, LengthUnit.Cells),    // fixed 10
                    Scalar(1.0, LengthUnit.Fraction),  // remainder ½
                    Scalar(1.0, LengthUnit.Fraction),  // remainder ½
                ),
            ),
        )
        placements[0].region.width shouldBe 10
        placements[1].region.width shouldBe 10  // (30 - 10) / 2
        placements[2].region.width shouldBe 10
    }

    "column-span on a child stretches it across multiple cells" {
        val a = Box(id = "a") to Styles(columnSpan = 2)
        val b = Box(id = "b") to Styles.NULL
        val c = Box(id = "c") to Styles.NULL
        val placements = GridLayout.arrangeWithParent(
            region = Region(0, 0, 30, 10),
            children = listOf(a, b, c),
            parentStyles = Styles(gridSize = 3 to 2),
        )
        // a spans cols 0–1; b goes to col 2 of row 0; c wraps to row 1 col 0.
        placements[0].region.x shouldBe 0
        placements[0].region.width shouldBe 20  // 2 columns × 10
        placements[1].region.x shouldBe 20
        placements[1].region.width shouldBe 10
        placements[2].region.x shouldBe 0
        placements[2].region.y shouldBe 5       // row 1
    }

    "row-span on a child stretches it vertically" {
        val tall = Box(id = "tall") to Styles(rowSpan = 2)
        val a = Box() to Styles.NULL
        val b = Box() to Styles.NULL
        val placements = GridLayout.arrangeWithParent(
            region = Region(0, 0, 20, 20),
            children = listOf(tall, a, b),
            parentStyles = Styles(gridSize = 2 to 2),
        )
        placements[0].region.height shouldBe 20  // both rows
        placements[0].region.x shouldBe 0
        placements[1].region.x shouldBe 10       // top-right
        placements[2].region.x shouldBe 10       // bottom-right
        placements[2].region.y shouldBe 10
    }

    "extra children beyond the declared grid extend into new rows" {
        val children = List(6) { Box() to Styles.NULL }
        val placements = GridLayout.arrangeWithParent(
            region = Region(0, 0, 20, 30),
            children = children,
            parentStyles = Styles(gridSize = 2 to 2),
        )
        placements.size shouldBe 6
        // 6 children in 2-col grid → 3 rows. The extra row consumes the
        // remaining vertical space evenly.
        val rows = placements.map { it.region.y }.distinct().sorted()
        rows.size shouldBe 3
    }
})
