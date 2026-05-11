package tools.konsole.textual.compositor

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import tools.konsole.rich.geometry.Region
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.widgets.Grid
import tools.konsole.textual.widgets.Horizontal
import tools.konsole.textual.widgets.Vertical

private class Box(id: String? = null) : Widget(id, emptySet())

class ContainerLayoutTest : StringSpec({

    "placing a Grid container recursively places its children" {
        val a = Box(id = "a")
        val b = Box(id = "b")
        val c = Box(id = "c")
        val d = Box(id = "d")
        val grid = Grid(cols = 2, rows = 2, children = listOf(a, b, c, d), id = "g")

        val compositor = Compositor(Region(0, 0, 40, 20))
        compositor.placeAt(grid, Region(0, 0, 40, 20))

        // 1 grid placement + 4 child placements.
        compositor.placements.size shouldBe 5

        val byWidget = compositor.placements.associateBy { it.widget }
        byWidget[grid]?.region shouldBe Region(0, 0, 40, 20)
        byWidget[a]?.region shouldBe Region(0, 0, 20, 10)
        byWidget[b]?.region shouldBe Region(20, 0, 20, 10)
        byWidget[c]?.region shouldBe Region(0, 10, 20, 10)
        byWidget[d]?.region shouldBe Region(20, 10, 20, 10)
    }

    "Vertical container stacks children top-to-bottom by default 1fr heights" {
        val a = Box(); val b = Box(); val c = Box()
        val v = Vertical(a, b, c)

        val compositor = Compositor(Region(0, 0, 30, 12))
        compositor.placeAt(v, Region(0, 0, 30, 12))

        val byWidget = compositor.placements.associateBy { it.widget }
        byWidget[a]?.region?.height shouldBe 4
        byWidget[b]?.region?.height shouldBe 4
        byWidget[c]?.region?.height shouldBe 4
        byWidget[a]?.region?.y shouldBe 0
        byWidget[b]?.region?.y shouldBe 4
        byWidget[c]?.region?.y shouldBe 8
    }

    "Horizontal container lays children left-to-right" {
        val a = Box(); val b = Box()
        val h = Horizontal(a, b)
        val compositor = Compositor(Region(0, 0, 20, 10))
        compositor.placeAt(h, Region(0, 0, 20, 10))
        val byWidget = compositor.placements.associateBy { it.widget }
        byWidget[a]?.region?.x shouldBe 0
        byWidget[b]?.region?.x shouldBe 10
        byWidget[a]?.region?.width shouldBe 10
        byWidget[b]?.region?.width shouldBe 10
    }

    "nested containers recurse correctly" {
        val inner1 = Box(id = "inner1")
        val inner2 = Box(id = "inner2")
        val innerH = Horizontal(inner1, inner2)
        val sidebar = Box(id = "sidebar")
        val root = Vertical(sidebar, innerH)

        val compositor = Compositor(Region(0, 0, 20, 10))
        compositor.placeAt(root, Region(0, 0, 20, 10))

        // root, sidebar, innerH, inner1, inner2 → 5 placements.
        compositor.placements.size shouldBe 5
        val byWidget = compositor.placements.associateBy { it.widget }
        byWidget[sidebar]?.region?.y shouldBe 0
        byWidget[sidebar]?.region?.height shouldBe 5
        byWidget[innerH]?.region?.y shouldBe 5
        byWidget[innerH]?.region?.height shouldBe 5
        // inner1 / inner2 split innerH's region horizontally.
        byWidget[inner1]?.region shouldBe Region(0, 5, 10, 5)
        byWidget[inner2]?.region shouldBe Region(10, 5, 10, 5)
    }
})
