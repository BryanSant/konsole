<img width="1800" height="600" alt="konsole" src="https://github.com/user-attachments/assets/9e183c2b-d9a6-4059-8147-a788a412412c" />

# konsole

A pure-Kotlin reimplementation of Python's [`rich`](https://github.com/Textualize/rich)
and [`textual`](https://github.com/Textualize/textual) libraries, built for the JVM
with direct FFM bindings to termios / ioctl — no JLine, no JNI.

## Modules

| Module               | Purpose                                                                |
|----------------------|------------------------------------------------------------------------|
| `:core`              | Terminal primitives — FFM-direct TTY, ANSI commands, input parser      |
| `:rich`              | `rich` port — Console, Style, Text, renderables, Live, Progress, etc.  |
| `:textualize`        | `textual` port — App, Screen, Widget, TCSS, built-in widgets           |
| `:examples`          | Runnable demos                                                         |

Published Maven artifacts keep the `konsole-` prefix: `konsole-core`, `konsole-rich`, `konsole-textual`.

## Stack

- Gradle 9.5.0, Kotlin 2.3.21, Java 25 toolchain
- kotlinx-coroutines 1.10.2, kotlinx-io 0.9.0
- commonmark 0.24.0 (Markdown), Kotest 6.1.11 (tests)

Runs on Linux and macOS today. Terminals that implement the kitty keyboard
protocol (e.g. kitty, Ghostty, WezTerm, Alacritty, recent iTerm2) get the
full input model; legacy CSI keys still work elsewhere. A Windows driver is
on the roadmap but not implemented yet — on Windows the runtime falls back
to konsole's dumb TTY (no raw mode, no input events).

## Running a demo

The `demo.sh` script wraps the Gradle invocation with the right JVM args
and lets you pick by short name:

```sh
./demo.sh              # list all demos
./demo.sh Pride        # run PrideApp (suffix optional, case-insensitive)
./demo.sh Calculator   # run CalculatorApp
```

Or directly:

```sh
./gradlew :examples:runExample -Pexample=PrideApp
```

Konsole's libc bindings on Java 25 need `--enable-native-access=ALL-UNNAMED`.
The `runExample` task and all `test` tasks set this automatically; downstream
consumers must pass it themselves.

## Development

- `./gradlew build` — compile + test (excludes TTY-required tests)
- `./gradlew :core:ttyTest` — opt-in real-terminal integration tests
- `./gradlew test` — fast unit tests across all modules

## License

MIT — see [LICENSE](LICENSE).
