package tools.konsole.textual.compositor

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import tools.konsole.rich.Renderable
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.geometry.Region
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.widgets.Label
import tools.konsole.textual.widgets.Toast
import tools.konsole.textual.widgets.Tooltip

private class Patch(public val ch: String) : Widget() {
    override fun render(): Renderable = Text(ch)
}

class CompositorTest : StringSpec({

    "arrange stacks widgets vertically on the BASE layer" {
        val c = Compositor(Region(0, 0, 20, 5))
        c.arrange(listOf(Label("a"), Label("b"), Label("c")))
        c.placements.size shouldBe 3
        c.placements[0].region.y shouldBe 0
        c.placements[1].region.y shouldBe 1
        c.placements[2].region.y shouldBe 2
        c.placements.all { it.layer == Compositor.BASE } shouldBe true
    }

    "placeBelow anchors an overlay just under the parent region" {
        val c = Compositor(Region(0, 0, 30, 20))
        val anchor = Region(2, 3, 10, 1)
        val region = c.placeBelow(Label("dropdown"), anchor, width = 10, height = 3)
        region.x shouldBe 2
        region.y shouldBe 4
        region.width shouldBe 10
        region.height shouldBe 3
    }

    "placeBelow flips above when there isn't room below" {
        val c = Compositor(Region(0, 0, 30, 10))
        val anchor = Region(2, 8, 10, 1)  // only 1 row below
        val region = c.placeBelow(Label("dropdown"), anchor, width = 10, height = 4)
        // height=4 can't fit below; flips above
        region.y shouldBe 4  // anchor.y(8) - height(4)
    }

    "placeBelow clamps width to the viewport" {
        val c = Compositor(Region(0, 0, 15, 10))
        val anchor = Region(10, 3, 20, 1)  // anchor extends beyond viewport
        val region = c.placeBelow(Label("dropdown"), anchor, width = 30, height = 3)
        region.right shouldBe 15  // clamped to viewport.right
    }

    "placeRightOf places to the right of the parent when there's room" {
        val c = Compositor(Region(0, 0, 30, 20))
        val anchor = Region(2, 3, 5, 1)
        val region = c.placeRightOf(Label("tooltip"), anchor, width = 10, height = 1)
        region.x shouldBe 7  // anchor.right
        region.y shouldBe 3
    }

    "hit-test returns the topmost widget at a point" {
        val c = Compositor(Region(0, 0, 30, 20))
        val base = Label("base")
        val overlay = Label("overlay")
        c.placeAt(base, Region(0, 0, 30, 20), Compositor.BASE)
        c.placeAt(overlay, Region(5, 5, 10, 5), Compositor.OVERLAY)
        c.hitTest(7, 7) shouldBe overlay
        c.hitTest(20, 15) shouldBe base
    }

    "hit-test returns null when nothing covers the point" {
        val c = Compositor(Region(0, 0, 30, 20))
        c.placeAt(Label("x"), Region(0, 0, 5, 5))
        c.hitTest(20, 10) shouldBe null
    }

    "render produces one Strip per viewport row" {
        val c = Compositor(Region(0, 0, 20, 4))
        c.arrange(listOf(Patch("A"), Patch("B"), Patch("C")))
        val strips = c.render()
        strips.size shouldBe 4
    }

    "higher layer overwrites lower at overlapping cells" {
        val c = Compositor(Region(0, 0, 5, 3))
        // BASE renders "X" only on its row 0 (Patch is single-line).
        c.placeAt(Patch("X"), Region(0, 0, 5, 1), Compositor.BASE)
        // OVERLAY renders "@" on the same row 0, columns 1..3.
        c.placeAt(Patch("@"), Region(1, 0, 3, 1), Compositor.OVERLAY)
        val strips = c.render()
        val topRow = strips[0].segments.joinToString("") { it.text }
        topRow[0] shouldBe 'X'         // BASE's only char
        topRow[1] shouldBe '@'         // OVERLAY overwrites cells 1..3
    }

    "clearLayer removes only that layer's placements" {
        val c = Compositor(Region(0, 0, 30, 20))
        c.placeAt(Label("base"), Region(0, 0, 5, 1), Compositor.BASE)
        c.placeAt(Label("tooltip"), Region(2, 2, 5, 1), Compositor.TOOLTIP)
        c.placements.size shouldBe 2
        c.clearLayer(Compositor.TOOLTIP)
        c.placements.size shouldBe 1
        c.placements[0].layer shouldBe Compositor.BASE
    }

    "removePlacement drops the named widget" {
        val c = Compositor(Region(0, 0, 30, 20))
        val w = Label("x")
        c.placeAt(w, Region(0, 0, 5, 1))
        c.removePlacement(w) shouldBe true
        c.placements.size shouldBe 0
    }

    "Toast advertises the TOAST layer via preferredLayer" {
        val t = Toast(title = "x", message = "y")
        t.preferredLayer shouldBe Compositor.TOAST
    }

    "Tooltip advertises the TOOLTIP layer" {
        val tt = Tooltip("hover me")
        tt.preferredLayer shouldBe Compositor.TOOLTIP
    }

    "Label advertises preferredLayer = null (caller chooses)" {
        val l = Label("x")
        l.preferredLayer shouldBe null
    }

    "compositor honours a subclass that overrides only renderLine (back-compat)" {
        // Pre-renderStrips widgets draw via renderLine; the default renderStrips
        // must detect the override and dispatch per row so they keep working
        // without being migrated. Regression for the AnimationDemo BarRow shape.
        class BarRow : Widget() {
            override fun render(): tools.konsole.rich.Renderable = tools.konsole.rich.Text("")
            override fun renderLine(y: Int, width: Int): tools.konsole.rich.Strip {
                if (y != 0) return tools.konsole.rich.Strip.EMPTY
                return tools.konsole.rich.Strip.of(
                    tools.konsole.rich.Segment("X".repeat(width))
                )
            }
        }
        val c = Compositor(Region(0, 0, 10, 1))
        c.placeAt(BarRow(), Region(0, 0, 10, 1))
        val strip = c.render().first()
        strip.segments.joinToString("") { it.text } shouldBe "XXXXXXXXXX"
    }
})
