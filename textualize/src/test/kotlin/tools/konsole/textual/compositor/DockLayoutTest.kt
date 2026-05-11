package tools.konsole.textual.compositor

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import tools.konsole.rich.geometry.Region
import tools.konsole.textual.css.Stylesheet
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.widgets.Vertical

private class DockBox(id: String? = null, classes: Set<String> = emptySet()) : Widget(id, classes)

class DockLayoutTest : StringSpec({

    "dock: top pins a child to the top edge with the declared height" {
        val header = DockBox(id = "header")
        val body = DockBox(id = "body")
        val v = Vertical(header, body)

        val compositor = Compositor(Region(0, 0, 20, 10))
        compositor.stylesheet = Stylesheet.parse(
            "#header { dock: top; height: 1; }"
        )
        compositor.placeAt(v, Region(0, 0, 20, 10))

        val byWidget = compositor.placements.associateBy { it.widget }
        byWidget[header]?.region shouldBe Region(0, 0, 20, 1)
        // body gets the residual after dock removed the top row.
        byWidget[body]?.region shouldBe Region(0, 1, 20, 9)
    }

    "dock: bottom pins a child to the bottom edge" {
        val footer = DockBox(id = "footer")
        val body = DockBox(id = "body")
        val v = Vertical(body, footer)
        val compositor = Compositor(Region(0, 0, 20, 10))
        compositor.stylesheet = Stylesheet.parse("#footer { dock: bottom; height: 1; }")
        compositor.placeAt(v, Region(0, 0, 20, 10))
        val byWidget = compositor.placements.associateBy { it.widget }
        byWidget[footer]?.region shouldBe Region(0, 9, 20, 1)
        byWidget[body]?.region shouldBe Region(0, 0, 20, 9)
    }

    "dock: left + dock: right + body works as a 3-pane chrome" {
        val sidebar = DockBox(id = "sidebar")
        val tools = DockBox(id = "tools")
        val main = DockBox(id = "main")
        val v = Vertical(sidebar, tools, main)
        val compositor = Compositor(Region(0, 0, 100, 20))
        compositor.stylesheet = Stylesheet.parse("""
            #sidebar { dock: left; width: 20; }
            #tools   { dock: right; width: 30; }
        """.trimIndent())
        compositor.placeAt(v, Region(0, 0, 100, 20))

        val byWidget = compositor.placements.associateBy { it.widget }
        byWidget[sidebar]?.region shouldBe Region(0, 0, 20, 20)
        byWidget[tools]?.region shouldBe Region(70, 0, 30, 20)
        byWidget[main]?.region shouldBe Region(20, 0, 50, 20)
    }

    "dock applies in declaration order so multiple top-docks stack" {
        val a = DockBox(id = "a")
        val b = DockBox(id = "b")
        val rest = DockBox(id = "rest")
        val v = Vertical(a, b, rest)
        val compositor = Compositor(Region(0, 0, 10, 10))
        compositor.stylesheet = Stylesheet.parse("""
            #a { dock: top; height: 1; }
            #b { dock: top; height: 2; }
        """.trimIndent())
        compositor.placeAt(v, Region(0, 0, 10, 10))
        val byWidget = compositor.placements.associateBy { it.widget }
        byWidget[a]?.region shouldBe Region(0, 0, 10, 1)
        byWidget[b]?.region shouldBe Region(0, 1, 10, 2)
        byWidget[rest]?.region shouldBe Region(0, 3, 10, 7)
    }
})
