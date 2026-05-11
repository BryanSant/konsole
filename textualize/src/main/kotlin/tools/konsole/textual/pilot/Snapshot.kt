package tools.konsole.textual.pilot

import java.io.File

/**
 * Minimal snapshot-testing helper. Mirrors `pytest-textual-snapshot` workflow
 * but adapted for kotest / JUnit5.
 *
 * Snapshots live in `src/test/resources/snapshots/<className>/<snapshotName>.txt`
 * (path resolved by the kotest spec relative to the test working directory).
 *
 * If `KONSOLE_UPDATE_SNAPSHOTS=true` is set in the environment, mismatches are
 * silently overwritten with the actual value — analogous to pytest's
 * `--snapshot-update`.
 */
public object Snapshot {

    private val updateMode: Boolean get() = System.getenv("KONSOLE_UPDATE_SNAPSHOTS") == "true"

    /** Compare [actual] against the snapshot at [path]. Throws on mismatch. */
    public fun assertMatches(actual: String, path: String) {
        val file = File(path)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.writeText(actual)
            println("[konsole-snapshot] created: $path")
            return
        }
        val expected = file.readText()
        if (expected == actual) return
        if (updateMode) {
            file.writeText(actual)
            println("[konsole-snapshot] updated: $path")
            return
        }
        throw AssertionError(
            "Snapshot mismatch at $path\n" +
                "--- expected ---\n$expected\n" +
                "--- actual ---\n$actual\n" +
                "(re-run with KONSOLE_UPDATE_SNAPSHOTS=true to overwrite)"
        )
    }
}

/** kotest-style infix matcher. */
public infix fun String.shouldMatchSnapshot(name: String) {
    val callerClass = Throwable().stackTrace
        .firstOrNull { it.className != "tools.konsole.textual.pilot.SnapshotKt" }
        ?.className
        ?: "anonymous"
    val safe = callerClass.replace('.', '_').replace('$', '_')
    Snapshot.assertMatches(this, "src/test/resources/snapshots/$safe/$name.txt")
}
