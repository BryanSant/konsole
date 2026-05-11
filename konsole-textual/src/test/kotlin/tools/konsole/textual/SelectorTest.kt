package tools.konsole.textual

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import tools.konsole.textual.dom.DOMNode
import tools.konsole.textual.dom.query
import tools.konsole.textual.dom.queryOne
import tools.konsole.textual.widget.Widget

private class Header(id: String? = null, classes: Set<String> = emptySet()) : Widget(id, classes)
private class Body(id: String? = null, classes: Set<String> = emptySet()) : Widget(id, classes)
private class Footer(id: String? = null, classes: Set<String> = emptySet()) : Widget(id, classes)

class SelectorTest : StringSpec({

    fun buildTree(): Body {
        val body = Body(id = "main")
        val header = Header(id = "hdr", classes = setOf("top"))
        val footer = Footer(id = "ftr")
        body.attach(header)
        body.attach(footer)
        return body
    }

    "queryOne by type returns the first matching node" {
        val root = buildTree()
        val h = root.queryOne("Header")
        (h is Header) shouldBe true
        h?.id shouldBe "hdr"
    }

    "queryOne by id returns matching node" {
        val root = buildTree()
        root.queryOne("#ftr")?.id shouldBe "ftr"
    }

    "queryOne by class returns matching node" {
        val root = buildTree()
        root.queryOne(".top")?.id shouldBe "hdr"
    }

    "query returns all matching nodes" {
        val root = buildTree()
        val all: List<DOMNode> = root.query("Header") + root.query("Footer")
        all.size shouldBe 2
    }

    "queryOne returns null when nothing matches" {
        val root = buildTree()
        root.queryOne("#nope") shouldBe null
    }
})
