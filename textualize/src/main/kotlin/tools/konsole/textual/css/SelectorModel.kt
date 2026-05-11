package tools.konsole.textual.css

import tools.konsole.textual.dom.DOMNode

/**
 * What a single [Selector] is matching against.
 * Mirrors Python textual's `model.SelectorType`.
 */
public enum class SelectorType { Universal, Type, Class, Id, PseudoClass }

/** Combinator joining adjacent compound selectors. */
public enum class CombinatorType { Descendant, Child, AdjacentSibling, GeneralSibling }

/**
 * A single atomic selector — one of `*`, `Widget`, `.foo`, `#bar`, `:hover`.
 * Mirrors textual's `model.Selector`.
 */
public data class Selector(
    public val type: SelectorType,
    public val name: String,
    public val pseudoClasses: Set<String> = emptySet(),
    public val combinator: CombinatorType = CombinatorType.Descendant,
) {
    /** Specificity contribution: id=100, class/pseudo=10, type=1, universal=0. */
    public val specificity: Specificity get() = when (type) {
        SelectorType.Id -> Specificity(1, pseudoClasses.size, 0)
        SelectorType.Class -> Specificity(0, 1 + pseudoClasses.size, 0)
        SelectorType.PseudoClass -> Specificity(0, 1 + pseudoClasses.size, 0)
        SelectorType.Type -> Specificity(0, pseudoClasses.size, 1)
        SelectorType.Universal -> Specificity(0, pseudoClasses.size, 0)
    }

    public fun matches(node: DOMNode, focused: Boolean = false, hovered: Boolean = false, disabled: Boolean = false): Boolean {
        if (!matchesType(node)) return false
        // All pseudo-classes (if any) must match.
        for (p in pseudoClasses) {
            val ok = when (p) {
                "focus" -> focused
                "hover" -> hovered
                "disabled" -> disabled
                "enabled" -> !disabled
                else -> false  // unknown pseudo — treat as non-match (forward compatible)
            }
            if (!ok) return false
        }
        return true
    }

    private fun matchesType(node: DOMNode): Boolean = when (type) {
        SelectorType.Universal -> true
        SelectorType.Type -> node.cssType == name
        SelectorType.Class -> name in node.classes
        SelectorType.Id -> node.id == name
        SelectorType.PseudoClass -> true  // standalone pseudo (rare); pseudo-only selectors are *:pseudo
    }
}

/** Three-component specificity (id, class+pseudo, type). Mirrors textual's `Specificity3`. */
public data class Specificity(val id: Int, val cls: Int, val type: Int) : Comparable<Specificity> {
    public operator fun plus(other: Specificity): Specificity =
        Specificity(id + other.id, cls + other.cls, type + other.type)

    override fun compareTo(other: Specificity): Int = when {
        id != other.id -> id.compareTo(other.id)
        cls != other.cls -> cls.compareTo(other.cls)
        else -> type.compareTo(other.type)
    }

    public companion object {
        public val ZERO: Specificity = Specificity(0, 0, 0)
    }
}

/**
 * A chain of [Selector]s combined by their [Selector.combinator]. e.g. `Widget.foo > Button`
 * parses to `[Selector(Widget, ...), Selector(Button, combinator=Child)]`.
 *
 * `matches(node)` walks ancestors right-to-left honoring each combinator.
 */
public data class SelectorChain(public val selectors: List<Selector>) {
    public val specificity: Specificity = selectors.fold(Specificity.ZERO) { a, s -> a + s.specificity }

    public fun matches(node: DOMNode): Boolean {
        if (selectors.isEmpty()) return false
        // Last selector must match the node itself.
        if (!selectors.last().matches(node)) return false
        if (selectors.size == 1) return true
        var idx = selectors.size - 2
        var current: DOMNode? = node.parent
        while (idx >= 0 && current != null) {
            val sel = selectors[idx + 1]
            when (sel.combinator) {
                CombinatorType.Child -> {
                    if (!selectors[idx].matches(current)) return false
                    idx -= 1
                    current = current.parent
                }
                CombinatorType.Descendant -> {
                    if (selectors[idx].matches(current)) {
                        idx -= 1
                    }
                    current = current.parent
                }
                CombinatorType.AdjacentSibling, CombinatorType.GeneralSibling -> {
                    // Sibling combinators not modelled in Phase 8; treat as non-match.
                    return false
                }
            }
        }
        return idx < 0
    }
}

/**
 * Comma-separated list of [SelectorChain]s. e.g. `Widget.foo, #main > Button` ⇒ two chains.
 * A node matches a [SelectorSet] iff it matches any chain.
 */
public data class SelectorSet(public val chains: List<SelectorChain>) {
    public val specificity: Specificity = chains.maxByOrNull { it.specificity }?.specificity ?: Specificity.ZERO
    public fun matches(node: DOMNode): Boolean = chains.any { it.matches(node) }
}
