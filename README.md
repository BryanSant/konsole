# konsole

A pure-Kotlin reimplementation of Python's [`rich`](https://github.com/Textualize/rich)
and [`textual`](https://github.com/Textualize/textual) libraries, built for the JVM
with native terminal access via [JLine FFM](https://github.com/jline/jline3).

## Modules

| Module               | Purpose                                                                |
|----------------------|------------------------------------------------------------------------|
| `:core`              | Terminal primitives — JLine wrapper, ANSI commands, input parser       |
| `:rich`              | `rich` port — Console, Style, Text, renderables, Live, Progress, etc.  |
| `:textualize`        | `textual` port — App, Screen, Widget, TCSS, built-in widgets           |
| `:examples`          | Runnable demos                                                         |

Published Maven artifacts keep the `konsole-` prefix: `konsole-core`, `konsole-rich`, `konsole-textual`.

## Stack

- Gradle 9.5.0, Kotlin 2.3.21, Java 25 toolchain
- JLine 4.1.0 (`jline-terminal` + `jline-terminal-ffm`)
- kotlinx-coroutines 1.10.2, kotlinx-io 0.9.0
- commonmark 0.24.0 (Markdown), Kotest 6.1.11 (tests)

Runs on Linux, macOS, and Windows Terminal 1.25+ (kitty keyboard protocol
support is required; legacy `conhost` is not supported).

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

JLine FFM on Java 25 needs `--enable-native-access=ALL-UNNAMED`. The
`runExample` task and all `test` tasks set this automatically; downstream
consumers must pass it themselves.

## Development

- `./gradlew build` — compile + test (excludes TTY-required tests)
- `./gradlew :core:ttyTest` — opt-in real-terminal integration tests
- `./gradlew test` — fast unit tests across all modules

## License

MIT — see [LICENSE](LICENSE).
