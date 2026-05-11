package tools.konsole.rich

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import tools.konsole.rich.geometry.Offset
import tools.konsole.rich.geometry.Region
import tools.konsole.rich.geometry.Spacing

class GeometryTest : StringSpec({

    "Offset addition" {
        (Offset(2, 3) + Offset(4, 5)) shouldBe Offset(6, 8)
        (-Offset(1, 2)) shouldBe Offset(-1, -2)
    }

    "Spacing aggregates width and height" {
        val s = Spacing(top = 1, right = 2, bottom = 3, left = 4)
        s.width shouldBe 6   // left + right
        s.height shouldBe 4  // top + bottom
    }

    "Spacing.symmetric / .all helpers" {
        Spacing.symmetric(horizontal = 2, vertical = 1) shouldBe Spacing(1, 2, 1, 2)
        Spacing.all(3) shouldBe Spacing(3, 3, 3, 3)
    }

    "Region geometry — right, bottom, area" {
        val r = Region(2, 3, 10, 5)
        r.right shouldBe 12
        r.bottom shouldBe 8
        r.area shouldBe 50
        r.isEmpty shouldBe false
    }

    "Empty region detection" {
        Region(0, 0, 0, 5).isEmpty shouldBe true
        Region(0, 0, 5, 0).isEmpty shouldBe true
        Region.EMPTY.isEmpty shouldBe true
    }

    "Region.contains an offset" {
        val r = Region(0, 0, 10, 10)
        (Offset(5, 5) in r) shouldBe true
        (Offset(10, 5) in r) shouldBe false  // exclusive on right edge
        (Offset(-1, 0) in r) shouldBe false
    }

    "Region.shrink with insets" {
        val r = Region(0, 0, 10, 10).shrink(Spacing(top = 1, right = 2, bottom = 3, left = 4))
        r shouldBe Region(4, 1, 4, 6)
    }

    "Region.intersection" {
        val a = Region(0, 0, 10, 10)
        val b = Region(5, 5, 10, 10)
        a.intersection(b) shouldBe Region(5, 5, 5, 5)
    }

    "Region.intersection of disjoint regions is empty" {
        val a = Region(0, 0, 5, 5)
        val b = Region(10, 10, 5, 5)
        a.intersection(b).isEmpty shouldBe true
    }
})
