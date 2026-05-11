package tools.konsole.textual.css

import tools.konsole.rich.LRUCache
import tools.konsole.textual.dom.DOMNode

/**
 * A loaded collection of [RuleSet]s. Mirrors textual's `Stylesheet`.
 *
 * Call [apply] to compute the merged [Styles] for a [DOMNode] — rules with
 * higher specificity overlay lower ones, ties broken by declaration order.
 *
 * Match results are memoised per `(node-identity, rule-index)` via [LRUCache];
 * stylesheets are typically small (10s of rules) and nodes update infrequently.
 */
public class Stylesheet(public val rules: List<RuleSet> = emptyList()) {

    private val matchCache: LRUCache<Pair<Int, Int>, Boolean> = LRUCache(capacity = 1024)

    public fun apply(node: DOMNode): Styles {
        // Match every rule against node, sort by specificity, then overlay.
        val matched = rules.withIndex().filter { (i, r) ->
            val key = System.identityHashCode(node) to i
            matchCache.getOrPut(key) { r.selectors.matches(node) }
        }
        val sorted = matched.sortedWith(compareBy({ it.value.specificity }, { it.index }))
        var styles = Styles.NULL
        for ((_, rule) in sorted) {
            styles = styles + StylesBuilder.build(rule.declarations)
        }
        return styles
    }

    /** Parse [source] into a [Stylesheet]. */
    public companion object {
        public fun parse(source: String, file: String = "<inline>"): Stylesheet =
            Stylesheet(Parser.parse(source, file))
    }

    /** Concatenation: rule sets from `other` apply after `this`. */
    public operator fun plus(other: Stylesheet): Stylesheet = Stylesheet(rules + other.rules)
}
