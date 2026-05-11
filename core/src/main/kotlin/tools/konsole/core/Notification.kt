package tools.konsole.core

import tools.konsole.core.Command
import tools.konsole.core.Ansi.OSC
import tools.konsole.core.Ansi.ST

/**
 * Send a desktop notification via OSC escape sequences.
 *
 * Emits three sequences in one write so whichever dialect the terminal
 * supports will trigger:
 *
 * - OSC 9   `ESC]9;<body>ST`                   ConEmu, Windows Terminal
 * - OSC 99  `ESC]99;<title>;<body>ST`           Kitty
 * - OSC 777 `ESC]777;notify;<title>;<body>ST`   urxvt + libnotify
 *
 * Terminals that do not implement any of these silently discard the sequences.
 */
public data class SendNotification(val title: String, val body: String) : Command {
    override fun writeAnsi(out: Appendable) {
        out.append(OSC).append("9;").append(body).append(ST)
        out.append(OSC).append("99;").append(title).append(';').append(body).append(ST)
        out.append(OSC).append("777;notify;").append(title).append(';').append(body).append(ST)
    }
}
