package tools.konsole.textual.compositor

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import tools.konsole.rich.geometry.Region
import tools.konsole.textual.css.Stylesheet
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.widgets.Horizontal
import tools.konsole.textual.widgets.Vertical

private class TestBox(id: String? = null, classes: Set<String> = emptySet()) : Widget(id, classes)

class CssCascadeTest : StringSpec({

    "CSS height overrides container's 1fr default" {
        // Vertical with no `heights` defaults every child to 1fr (even split).
        // CSS rule `#tall { height: 6; }` should pin one child to 6 cells,
        // the other gets the remaining 4.
        val tall = TestBox(id = "tall")
        val rest = TestBox(id = "rest")
        val v = Vertical(tall, rest)

        val compositor = Compositor(Region(0, 0, 10, 10))
        compositor.stylesheet = Stylesheet.parse("#tall { height: 6; }")
        compositor.placeAt(v, Region(0, 0, 10, 10))

        val byWidget = compositor.placements.associateBy { it.widget }
        byWidget[tall]?.region?.height shouldBe 6
        byWidget[rest]?.region?.height shouldBe 4
    }

    "CSS width matched by class selector overrides Horizontal's 1fr" {
        val a = TestBox(classes = setOf("narrow"))
        val b = TestBox()
        val h = Horizontal(a, b)

        val compositor = Compositor(Region(0, 0, 20, 5))
        compositor.stylesheet = Stylesheet.parse(".narrow { width: 8; }")
        compositor.placeAt(h, Region(0, 0, 20, 5))

        val byWidget = compositor.placements.associateBy { it.widget }
        byWidget[a]?.region?.width shouldBe 8
        byWidget[b]?.region?.width shouldBe 12   // 20 - 8 of fraction
    }

    "no stylesheet falls back to container-supplied childStyles" {
        // Vertical without an explicit `heights` defaults to 1fr per child.
        // No stylesheet → children split the viewport evenly.
        val a = TestBox(); val b = TestBox()
        val v = Vertical(a, b)
        val compositor = Compositor(Region(0, 0, 10, 10))
        // Note: no compositor.stylesheet set.
        compositor.placeAt(v, Region(0, 0, 10, 10))
        val byWidget = compositor.placements.associateBy { it.widget }
        byWidget[a]?.region?.height shouldBe 5
        byWidget[b]?.region?.height shouldBe 5
    }
})
