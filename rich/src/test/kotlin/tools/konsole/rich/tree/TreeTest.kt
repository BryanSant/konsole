package tools.konsole.rich.tree

import tools.konsole.core.ColorSystem
import tools.konsole.rich.Console
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.StringWriter

class TreeTest : StringSpec({

    "single root tree" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        c.print(tree("root"))
        sw.toString() shouldBe "root\n"
    }

    "tree with children uses guides" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        val t = tree("root").apply {
            add("a")
            add("b")
            add("c")
        }
        c.print(t)
        val out = sw.toString()
        out shouldContain "root"
        out shouldContain "├── a"
        out shouldContain "├── b"
        out shouldContain "└── c"
    }

    "deeply nested tree shows continuing branch markers" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        val t = tree("root").apply {
            val a = add("a")
            a.add("a1")
            a.add("a2")
            add("b")
        }
        c.print(t)
        val out = sw.toString()
        out shouldContain "├── a"
        out shouldContain "│   ├── a1"
        out shouldContain "│   └── a2"
        out shouldContain "└── b"
    }

    "hideRoot omits root label and indents children at column 0" {
        val sw = StringWriter()
        val c = Console(terminal = null, writer = sw, width = 80, colorSystem = ColorSystem.None)
        val t = Tree(tools.konsole.rich.Text("root"), hideRoot = true).apply {
            add("a")
            add("b")
        }
        c.print(t)
        val out = sw.toString()
        out shouldContain "├── a"
        out shouldContain "└── b"
        // root text is omitted (and so is the trailing newline that root would have produced)
        out.lines().none { it.startsWith("root") } shouldBe true
    }
})
