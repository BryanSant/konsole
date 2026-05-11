package tools.konsole.examples

import tools.konsole.textual.app.App
import tools.konsole.textual.binding.bindings
import tools.konsole.textual.css.LengthUnit
import tools.konsole.textual.css.Scalar
import tools.konsole.textual.driver.HeadlessDriver
import tools.konsole.textual.driver.systemDriver
import tools.konsole.textual.widgets.Command
import tools.konsole.textual.widgets.CommandPalette
import tools.konsole.textual.screen.ConfirmScreen
import tools.konsole.textual.screen.InputScreen
import tools.konsole.textual.widget.Widget
import tools.konsole.textual.widgets.Banner
import tools.konsole.textual.widgets.BannerSeverity
import tools.konsole.textual.widgets.Footer
import tools.konsole.textual.widgets.Header
import tools.konsole.textual.widgets.Label
import tools.konsole.textual.widgets.Slider
import tools.konsole.textual.widgets.Static
import tools.konsole.textual.widgets.Vertical

/**
 * Showcase for the modal screens, command palette, slider, and banner.
 *
 *   ./demo.sh Dialog
 *
 * Keys:
 *  - `c`        — open a Confirm dialog
 *  - `i`        — open an Input dialog
 *  - `p`        — open the Command palette
 *  - `b`        — toggle a banner
 *  - arrows + space — drive the slider
 *  - `q` / Ctrl+C — quit
 */
public class DialogDemo(headless: Boolean = false) : App(if (headless) HeadlessDriver() else systemDriver()) {

    private val status = Label("Press c / i / p / b to open dialogs. Click 'q' to quit.", id = "status")
    private val slider = Slider(value = 50.0, min = 0.0, max = 100.0, id = "slider")
    private val banner = Banner("Heads up — this is a sample banner.", severity = BannerSeverity.Warning)
    private var bannerVisible = false

    override val bindings = bindings(
        "q" to "quit",
        "ctrl+c" to "quit",
        "c" to "open_confirm",
        "i" to "open_input",
        "p" to "open_palette",
        "b" to "toggle_banner",
    )

    @Suppress("unused")
    public fun action_open_confirm() {
        val dialog = ConfirmScreen(
            prompt = "Quit without saving?",
            title = "Discard?",
        )
        dialog.onResult { answer ->
            status.update("Confirm result: $answer")
            requestRefresh()
        }
        pushScreen(dialog)
    }

    @Suppress("unused")
    public fun action_open_input() {
        val dialog = InputScreen(
            prompt = "What's your name?",
            title = "Greeting",
            placeholder = "Type a name and press Enter",
        )
        dialog.onResult { name ->
            status.update(if (name.isNullOrBlank()) "Input cancelled." else "Hello, $name!")
            requestRefresh()
        }
        pushScreen(dialog)
    }

    @Suppress("unused")
    public fun action_open_palette() {
        val palette = CommandPalette(
            commands = listOf(
                Command("open", "Open File", "Open a file by path", listOf("file", "open")),
                Command("save", "Save", "Save the current file"),
                Command("close", "Close File", "Close the current file"),
                Command("quit", "Quit", "Exit the application", listOf("exit", "bye")),
                Command("toggle-banner", "Toggle Banner", "Show or hide the demo banner", listOf("notification")),
                Command("randomize-slider", "Randomize Slider", "Pick a random slider position"),
            ),
        )
        palette.onResult { picked ->
            picked ?: return@onResult
            when (picked.id) {
                "toggle-banner" -> action_toggle_banner()
                "randomize-slider" -> slider.setValue(Math.random() * 100.0)
                "quit" -> exit()
                else -> status.update("Picked: ${picked.title}")
            }
            requestRefresh()
        }
        pushScreen(palette)
    }

    @Suppress("unused")
    public fun action_toggle_banner() {
        bannerVisible = !bannerVisible
        requestRefresh()
    }

    init {
        slider.start()
        slider.onMessage<Slider.Changed> { status.update("Slider: %.1f".format(it.value)) }
    }

    override fun compose(): Sequence<Widget> = sequenceOf(
        Vertical(
            children = buildList {
                add(Header(title = "Dialogs / palette / slider / banner"))
                if (bannerVisible) add(banner)
                add(Static(""))
                add(status)
                add(Static(""))
                add(Label("Slider:"))
                add(slider)
                add(Static(""))
                add(Footer(this@DialogDemo.bindings))
            },
            heights = buildList {
                add(Scalar(1.0, LengthUnit.Cells))                  // header
                if (bannerVisible) add(Scalar(1.0, LengthUnit.Cells)) // banner
                add(Scalar(1.0, LengthUnit.Cells))                  // spacer
                add(Scalar(1.0, LengthUnit.Cells))                  // status
                add(Scalar(1.0, LengthUnit.Cells))                  // spacer
                add(Scalar(1.0, LengthUnit.Cells))                  // slider label
                add(Scalar(1.0, LengthUnit.Cells))                  // slider
                add(Scalar(1.0, LengthUnit.Fraction))               // bottom spacer
                add(Scalar(1.0, LengthUnit.Cells))                  // footer
            },
        )
    )
}

public fun main() {
    DialogDemo().run()
}
