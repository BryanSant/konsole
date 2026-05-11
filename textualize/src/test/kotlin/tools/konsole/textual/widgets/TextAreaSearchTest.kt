package tools.konsole.textual.widgets

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TextAreaSearchTest : StringSpec({

    "search finds every occurrence and jumps to the first" {
        val ta = TextArea(initial = "one two\nthree two\nfour")
        ta.search("two")
        ta.searchMatches.size shouldBe 2
        ta.activeMatchIndex shouldBe 0
        ta.searchMatches[0].start shouldBe Location(0, 4)
        ta.searchMatches[1].start shouldBe Location(1, 6)
        ta.cursor shouldBe Location(0, 4)
    }

    "nextMatch cycles through matches and wraps around" {
        val ta = TextArea(initial = "a b a b a")
        ta.search("a")
        ta.activeMatchIndex shouldBe 0
        ta.nextMatch(); ta.activeMatchIndex shouldBe 1
        ta.nextMatch(); ta.activeMatchIndex shouldBe 2
        ta.nextMatch(); ta.activeMatchIndex shouldBe 0   // wrap
    }

    "previousMatch wraps backwards" {
        val ta = TextArea(initial = "a b a b a")
        ta.search("a")
        ta.previousMatch()
        ta.activeMatchIndex shouldBe 2
    }

    "search with ignoreCase finds mixed-case occurrences" {
        val ta = TextArea(initial = "Hello\nWORLD\nhello")
        ta.search("hello", ignoreCase = true)
        ta.searchMatches.size shouldBe 2
    }

    "empty or null query clears search state" {
        val ta = TextArea(initial = "foo bar")
        ta.search("foo")
        ta.searchMatches.size shouldBe 1
        ta.search("")
        ta.searchMatches.size shouldBe 0
        ta.activeMatchIndex shouldBe -1
        ta.searchQuery shouldBe null
    }

    "clearSearch removes highlight state without touching the cursor" {
        val ta = TextArea(initial = "alpha beta gamma")
        ta.search("beta")
        val cursorBeforeClear = ta.cursor
        ta.clearSearch()
        ta.searchMatches.isEmpty() shouldBe true
        ta.cursor shouldBe cursorBeforeClear
    }
})
