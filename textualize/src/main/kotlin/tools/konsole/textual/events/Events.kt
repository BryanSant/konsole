package tools.konsole.textual.events

import tools.konsole.core.event.KeyCode
import tools.konsole.core.event.KeyModifiers
import tools.konsole.textual.message.Message

/**
 * Marker base for input/lifecycle events. Mirrors textual's `events.Event`.
 *
 * Events are the lowest-level messages — they originate from the [tools.konsole.textual.driver.Driver]
 * or from internal lifecycle transitions (mount, unmount, resize) and are dispatched
 * by the [tools.konsole.textual.message.MessagePump] to focused widgets and bubbled up.
 */
public open class Event : Message()

/** Keyboard input. Maps konsole-core's [tools.konsole.core.event.KeyEvent]. */
public data class Key(
    val code: KeyCode,
    val modifiers: KeyModifiers = KeyModifiers.NONE,
) : Event() {
    /** True if this key event matches the given [name] like `"ctrl+c"`, `"escape"`, etc. */
    public fun matches(name: String): Boolean {
        val lower = name.lowercase()
        // The literal '+' character is also our modifier separator, so a
        // naive split("+") would make it unbindable. Handle the corner cases:
        //   "+"        → keyPart='+', no mods
        //   "shift++"  → keyPart='+', mods={shift}      (trailing '++')
        //   "ctrl+c"   → keyPart='c', mods={ctrl}       (normal case)
        val (keyPart, modString) = when {
            lower == "+" -> "+" to ""
            lower.endsWith("++") -> "+" to lower.dropLast(2)
            else -> {
                val idx = lower.lastIndexOf('+')
                if (idx < 0) lower to ""
                else lower.substring(idx + 1) to lower.substring(0, idx)
            }
        }
        val mods = if (modString.isEmpty()) emptySet() else modString.split("+").toSet()
        if (mods.contains("ctrl") != modifiers.hasControl()) return false
        if (mods.contains("alt") != modifiers.hasAlt()) return false
        if (mods.contains("shift") != modifiers.hasShift()) return false
        return keyMatches(code, keyPart)
    }
    private fun keyMatches(c: KeyCode, key: String): Boolean = when (c) {
        is KeyCode.Char -> {
            val direct = c.c.lowercaseChar().toString() == key
            direct || (c.c == ' ' && key == "space") || (c.c == '\t' && key == "tab")
        }
        is KeyCode.F -> "f${c.n}" == key
        else -> c::class.simpleName?.lowercase() == key
    }
}

/**
 * Mouse button pressed at `(x, y)`. Mirrors Python textual's `MouseDown`.
 * Apps that need to detect drag start subscribe to this; the higher-level
 * [Click] event still fires when the user releases over the same target.
 */
public data class MouseDown(val x: Int, val y: Int, val button: MouseButton = MouseButton.Left) : Event()

/** Mouse button released at `(x, y)`. Mirrors Python textual's `MouseUp`. */
public data class MouseUp(val x: Int, val y: Int, val button: MouseButton = MouseButton.Left) : Event()

/**
 * Composite "click" — fires after [MouseDown] then [MouseUp] over the same
 * widget. Most apps subscribe to this and ignore the lower-level Down/Up
 * pair. Equivalent to a `mouseup` on the originally-pressed target in DOM
 * semantics.
 */
public data class Click(val x: Int, val y: Int, val button: MouseButton = MouseButton.Left) : Event()

public enum class MouseButton { Left, Middle, Right, WheelUp, WheelDown }

/** A pointer move. */
public data class MouseMove(val x: Int, val y: Int) : Event()

/**
 * Pointer dragged while a button is held — fires on every [MouseMove] that
 * happens between [MouseDown] and [MouseUp]. Carries the button that was
 * held and the cell coordinates of the pointer at the moment of the move.
 * Mirrors textual's `MouseScrollDown`/`MouseScrollUp`-adjacent semantics.
 */
public data class MouseDrag(
    val x: Int,
    val y: Int,
    val button: MouseButton,
    val startX: Int,
    val startY: Int,
) : Event()

/** Terminal resized. Carries the new size in cells. */
public data class Resize(val columns: Int, val rows: Int) : Event()

/** Sent to a widget when it is first attached to the tree. */
public class Mount : Event()

/** Sent to a widget when it is detached. */
public class Unmount : Event()

/** Sent when a widget gains keyboard focus. */
public class Focus : Event()

/** Sent when a widget loses keyboard focus. */
public class Blur : Event()

/** Sent when the terminal window gains focus. */
public class AppFocus : Event()

/** Sent when the terminal window loses focus. */
public class AppBlur : Event()

/** Bracketed paste payload. */
public data class Paste(val text: String) : Event()

/** Periodic timer tick — scheduled via [tools.konsole.textual.message.MessagePump.setTimer] / `setInterval`. */
public class Timer : Event()
