# konsole

A pure-Kotlin reimplementation of Python's [`rich`](https://github.com/Textualize/rich)
and [`textual`](https://github.com/Textualize/textual) libraries, built for the JVM
with native terminal access via [JLine FFM](https://github.com/jline/jline3).

> **Status:** Phase 0 — bootstrap. Not yet functional.

## Modules

| Module               | Purpose                                                                |
|----------------------|------------------------------------------------------------------------|
| `:core`              | Terminal primitives — JLine wrapper, ANSI commands, input parser       |
| `:rich`              | `rich` port — Console, Style, Text, renderables, Live, Progress, etc.  |
| `:textualize`        | `textual` port — App, Screen, Widget, TCSS, built-in widgets           |

(Published Maven artifacts keep the `konsole-` prefix: `konsole-core`, `konsole-rich`, `konsole-textual`.)
| `:examples`          | Runnable demos                                                         |

## Stack

- Gradle 9.5.0, Kotlin 2.3.21, Java 25 toolchain
- JLine 4.1.0 (`jline-terminal` + `jline-terminal-ffm`)
- kotlinx-coroutines 1.10.2, kotlinx-io 0.9.0
- commonmark 0.24.0 (Markdown), Kotest 6.1.11 (tests)

## Running an example

```sh
./gradlew :examples:runExample -Pexample=HelloKonsole
```

JLine FFM on Java 25 needs `--enable-native-access=ALL-UNNAMED`. The
`runExample` task and all `test` tasks set this automatically; downstream
consumers must pass it themselves.

## Development

- `./gradlew build` — compile + test (excludes TTY-required tests)
- `./gradlew :core:ttyTest` — opt-in real-terminal integration tests

## License

MIT — see [LICENSE](LICENSE).
