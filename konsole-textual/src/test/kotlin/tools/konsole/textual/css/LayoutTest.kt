package tools.konsole.textual.css

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import tools.konsole.rich.geometry.Region
import tools.konsole.textual.layouts.HorizontalLayout
import tools.konsole.textual.layouts.VerticalLayout
import tools.konsole.textual.widget.Widget

private class Box : Widget()

class LayoutTest : StringSpec({

    "VerticalLayout: explicit heights stack top-to-bottom" {
        val parent = Region(0, 0, 30, 20)
        val placements = VerticalLayout.arrange(
            parent,
            listOf(
                Box() to Styles(height = Scalar(5.0, Unit.Cells)),
                Box() to Styles(height = Scalar(8.0, Unit.Cells)),
            ),
        )
        placements.size shouldBe 2
        placements[0].region.y shouldBe 0
        placements[0].region.height shouldBe 5
        placements[1].region.y shouldBe 5
        placements[1].region.height shouldBe 8
    }

    "VerticalLayout: fraction units share remaining height" {
        val parent = Region(0, 0, 30, 30)
        val placements = VerticalLayout.arrange(
            parent,
            listOf(
                Box() to Styles(height = Scalar(10.0, Unit.Cells)),
                Box() to Styles(height = Scalar(1.0, Unit.Fraction)),
                Box() to Styles(height = Scalar(1.0, Unit.Fraction)),
            ),
        )
        // 30 - 10 = 20 remaining, 2 fractions ⇒ each 10
        placements[1].region.height shouldBe 10
        placements[2].region.height shouldBe 10
    }

    "VerticalLayout: skip display:none children" {
        val parent = Region(0, 0, 10, 10)
        val placements = VerticalLayout.arrange(
            parent,
            listOf(
                Box() to Styles(display = Display.None),
                Box() to Styles(height = Scalar(3.0, Unit.Cells)),
            ),
        )
        placements.size shouldBe 1
        placements[0].region.y shouldBe 0
    }

    "HorizontalLayout: explicit widths stack left-to-right" {
        val parent = Region(0, 0, 30, 10)
        val placements = HorizontalLayout.arrange(
            parent,
            listOf(
                Box() to Styles(width = Scalar(10.0, Unit.Cells)),
                Box() to Styles(width = Scalar(15.0, Unit.Cells)),
            ),
        )
        placements[0].region.x shouldBe 0
        placements[0].region.width shouldBe 10
        placements[1].region.x shouldBe 10
        placements[1].region.width shouldBe 15
    }
})
