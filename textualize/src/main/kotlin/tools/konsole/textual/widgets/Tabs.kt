package tools.konsole.textual.widgets

import tools.konsole.core.style.Color
import tools.konsole.rich.Renderable
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.markup.Markup
import tools.konsole.textual.binding.Binding
import tools.konsole.textual.binding.BindingsMap
import tools.konsole.textual.message.Message
import tools.konsole.textual.widget.Widget

/**
 * A single tab in a [Tabs] strip. Mirrors Python textual's `Tab`.
 *
 * Tabs carry an [id], a [label] (markup-parsed for display), and a
 * [disabled] state. The active tab is tracked by its parent [Tabs] widget.
 */
public data class Tab(
    public val id: String,
    public val label: String,
    public var disabled: Boolean = false,
)

/**
 * Horizontal tab strip. Mirrors Python textual's `Tabs`.
 *
 * Posts [TabActivated] whenever the active tab changes; arrow keys move
 * the active tab (skipping disabled ones).
 *
 * @param tabs initial tab definitions.
 * @param active id of the tab to start active, or `null` for the first non-disabled tab.
 */
public open class Tabs(
    tabs: List<Tab>,
    active: String? = null,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    public constructor(vararg tabs: Tab, active: String? = null, id: String? = null, classes: Set<String> = emptySet()) :
        this(tabs.toList(), active, id, classes)

    private val _tabs: MutableList<Tab> = tabs.toMutableList()
    public val tabs: List<Tab> get() = _tabs.toList()

    public var activeTabId: String? = active ?: _tabs.firstOrNull { !it.disabled }?.id
        private set

    public val activeTab: Tab? get() = activeTabId?.let { id -> _tabs.firstOrNull { it.id == id } }

    public val activeIndex: Int get() = _tabs.indexOfFirst { it.id == activeTabId }

    override val canFocus: Boolean get() = true

    override val bindings: BindingsMap = BindingsMap(
        listOf(
            Binding("left", "previous_tab", show = false),
            Binding("right", "next_tab", show = false),
            Binding("home", "first_tab", show = false),
            Binding("end", "last_tab", show = false),
        )
    )

    /** Append a tab. Returns its id. */
    public open fun addTab(tab: Tab): String {
        _tabs += tab
        if (activeTabId == null && !tab.disabled) {
            activeTabId = tab.id
            post(TabActivated(this, tab.id))
        }
        refresh()
        return tab.id
    }

    public fun addTab(id: String, label: String, disabled: Boolean = false): String =
        addTab(Tab(id, label, disabled))

    public open fun removeTab(tabId: String): Boolean {
        val idx = _tabs.indexOfFirst { it.id == tabId }
        if (idx < 0) return false
        _tabs.removeAt(idx)
        if (activeTabId == tabId) {
            activeTabId = _tabs.firstOrNull { !it.disabled }?.id
            activeTabId?.let { post(TabActivated(this, it)) }
        }
        refresh()
        return true
    }

    /** Switch to the tab with [tabId]. Idempotent; no-op if disabled. */
    public open fun activate(tabId: String) {
        val tab = _tabs.firstOrNull { it.id == tabId } ?: return
        if (tab.disabled) return
        if (activeTabId == tabId) return
        activeTabId = tabId
        post(TabActivated(this, tabId))
        refresh()
    }

    public fun disable(tabId: String): Boolean {
        val tab = _tabs.firstOrNull { it.id == tabId } ?: return false
        tab.disabled = true
        if (activeTabId == tabId) activate(_tabs.firstOrNull { !it.disabled }?.id ?: return true)
        refresh()
        return true
    }

    public fun enable(tabId: String): Boolean {
        val tab = _tabs.firstOrNull { it.id == tabId } ?: return false
        tab.disabled = false
        refresh()
        return true
    }

    public fun nextTab() {
        if (_tabs.isEmpty()) return
        val start = activeIndex
        var i = (start + 1) % _tabs.size
        while (i != start && _tabs[i].disabled) i = (i + 1) % _tabs.size
        if (i != start) activate(_tabs[i].id)
    }

    public fun previousTab() {
        if (_tabs.isEmpty()) return
        val start = activeIndex
        var i = (start - 1 + _tabs.size) % _tabs.size
        while (i != start && _tabs[i].disabled) i = (i - 1 + _tabs.size) % _tabs.size
        if (i != start) activate(_tabs[i].id)
    }

    override fun render(): Renderable {
        val text = Text()
        for ((i, tab) in _tabs.withIndex()) {
            if (i > 0) text.append("  ", Style.NULL)
            val active = tab.id == activeTabId
            val style = when {
                tab.disabled -> Style(color = Color.DarkGrey, dim = true)
                active -> Style(color = Color.White, bgcolor = Color.Rgb(0x00, 0x4f, 0x9f), bold = true)
                else -> Style(color = Color.Cyan)
            }
            text.append(" ${tab.label} ", style)
        }
        return text
    }

    /** Emitted when the active tab changes. */
    public data class TabActivated(val tabs: Tabs, val tabId: String) : Message()
}

/**
 * One pane inside a [TabbedContent]. Mirrors Python textual's `TabPane`.
 *
 * Children declared as the pane's contents are mounted under it; the
 * compositor reveals only the active pane's children.
 */
public open class TabPane(
    public val tabId: String,
    public val label: String,
    public val contents: List<Widget> = emptyList(),
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {
    init { for (c in contents) attach(c) }
    override fun render(): Renderable {
        // Render contents stacked vertically.
        val text = Text()
        for ((i, c) in contents.withIndex()) {
            for (seg in c.render().render(tools.konsole.rich.Console.string(width = 80), tools.konsole.rich.RenderOptions(maxWidth = 80))) {
                text.append(seg.text, seg.style)
            }
            if (i < contents.lastIndex) text.append("\n")
        }
        return text
    }
}

/**
 * A [Tabs] strip with paired [TabPane] content panels. Mirrors Python textual's `TabbedContent`.
 *
 * Only the active pane is rendered. Use [active]/[switchTo] to programmatically
 * change which pane is shown.
 */
public open class TabbedContent(
    public val panes: List<TabPane>,
    initialActive: String? = null,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    public val tabs: Tabs = Tabs(
        panes.map { Tab(it.tabId, it.label) },
        active = initialActive ?: panes.firstOrNull()?.tabId,
    )

    init {
        attach(tabs)
        for (p in panes) attach(p)
        tabs.start()
        tabs.onMessage<Tabs.TabActivated> { post(it) }
    }

    /** Currently visible pane. */
    public val activePane: TabPane?
        get() = panes.firstOrNull { it.tabId == tabs.activeTabId }

    public fun switchTo(tabId: String) {
        tabs.activate(tabId)
    }

    override fun render(): Renderable {
        val text = Text()
        for (seg in tabs.render().render(tools.konsole.rich.Console.string(width = 80), tools.konsole.rich.RenderOptions(maxWidth = 80))) {
            text.append(seg.text, seg.style)
        }
        text.append("\n")
        activePane?.let { p ->
            for (seg in p.render().render(tools.konsole.rich.Console.string(width = 80), tools.konsole.rich.RenderOptions(maxWidth = 80))) {
                text.append(seg.text, seg.style)
            }
        }
        return text
    }
}

/**
 * Switch between named child widgets, showing one at a time. Mirrors Python
 * textual's `ContentSwitcher`. The simpler cousin of [TabbedContent] —
 * no tab strip, just programmatic switching.
 */
public open class ContentSwitcher(
    public val contents: Map<String, Widget>,
    initial: String? = null,
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    public var current: String? = initial ?: contents.keys.firstOrNull()
        private set

    init { for (w in contents.values) attach(w) }

    public fun switchTo(key: String) {
        if (!contents.containsKey(key)) return
        if (current == key) return
        current = key
        refresh()
    }

    override fun render(): Renderable = current?.let { contents[it] }?.render() ?: Text("")
}
